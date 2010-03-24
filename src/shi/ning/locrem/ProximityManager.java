package shi.ning.locrem;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import shi.ning.locrem.ProximityManagerService.Stub;
import shi.ning.locrem.ReminderEntry.Columns;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

public final class ProximityManager extends Service {
    private final static String TAG = "ProximityManager";

    private final static int SERVICE_ALARM = 0;

    private final static int MIN_TIME = 300000; // 5 minutes
    private final static int MIN_DISTANCE = 200; // 200 meters
    private final static int RANGE = 500; // 500 meters

    private final static Criteria mCriteria = new Criteria();
    static {
        mCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
    }

    private final Stub mBinder = new Stub() {
        @Override
        public void onEntryChanged(long id) throws RemoteException {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "remote access, entry " + id + " changed");

            ProximityManager.this.onEntryChanged(id);
        }
    };

    private Context mContext;
    private Geocoder mGeocoder;
    private LocationManager mManager;
    private ProximityListener mListener;

    private final class ProximityListener implements LocationListener {
        @Override
        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {
            // XXX I'm not sure if I care about this
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "provider " + provider + " status changed to "
                      + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "provider " + provider + " enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, provider + " disabled");
            unregister();
            register();
        }

        @Override
        public void onLocationChanged(Location location) {
            final Address currentAddress = locationToAddress(location);
            final Time now = new Time();
            now.setToNow();

            checkAllEntry(now, currentAddress);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mManager =
            (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mGeocoder = new Geocoder(mContext);
        mListener = new ProximityListener();
        register();
        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: maybe we should check for redelivery
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "starting service with flags " + flags
                  + " and id " + startId);

        if (intent != null) {
            final long id = intent.getLongExtra(Columns._ID, -1);
            if (id >= 0) {
                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG, "previously scheduled entry " + id
                          + " is waken");

                onEntryChanged(id);
            }
        }

        return START_STICKY;
    }

    public Address locationToAddress(Location location) {
        try {
            if (location == null) {
                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG, "cannot reverse geocode null location");
                return null;
            }
            final List<Address> addresses =
                mGeocoder.getFromLocation(location.getLatitude(),
                                          location.getLongitude(),
                                          1);
            if (addresses != null && addresses.size() == 1)
                return addresses.get(0);
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "failed reverse geocoding " + location.toString());
            return null;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "error reverse geocoding " + location.toString());
            return null;
        }
    }

    private void unregister() {
        mManager.removeUpdates(mListener);
    }

    private void register() {
        final String provider = mManager.getBestProvider(mCriteria, true);
        /*
         * TODO Should ask user if they want to enable location service
         * if no provider is available.
         */
        if (provider != null) {
            mManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE,
                                            mListener);
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "registered to location provider " + provider);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "no location provider available");
        }
    }

    private void onEntryChanged(long id) {
        final Uri uri =
            ContentUris.withAppendedId(ReminderProvider.CONTENT_URI, id);
        final Cursor cursor =
            getContentResolver().query(uri, null, null, null, null);

        if (cursor.moveToFirst()) {
            final ReminderEntry entry = ReminderProvider.cursorToEntry(cursor);
            cursor.close();
            final Time now = new Time();
            now.setToNow();
            final String provider = mManager.getBestProvider(mCriteria, true);
            final Address current =
                locationToAddress(mManager.getLastKnownLocation(provider));
            checkEntry(entry, now, current);
        }
    }

    private void checkAllEntry(Time now, Address current) {
        final Cursor cursor =
            getContentResolver().query(ReminderProvider.ENABLED_URI,
                                       null, null, null, null);
        final LinkedList<ReminderEntry> entries =
            ReminderProvider.cursorToEntries(cursor);
        cursor.close();

        if (current == null || entries == null)
            return;

        for (ReminderEntry entry : entries) {
            checkEntry(entry, now, current);
        }
    }

    private void checkEntry(ReminderEntry entry, Time now, Address current) {
        if (entry.time.after(now)) {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "entry " + entry.id + " is scheduled to run after "
                      + entry.time.format("%F %T"));

            final Intent i = new Intent(this, ProximityManager.class);
            i.putExtra(Columns._ID, entry.id);
            final PendingIntent pi =
                PendingIntent.getService(mContext, SERVICE_ALARM, i,
                                         PendingIntent.FLAG_ONE_SHOT);
            final AlarmManager alarmManager =
                (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                             entry.time.toMillis(false),
                             pi);

            return;
        }

        for (Address a : entry.addresses) {
            if (inRange(current, a)) {
                // Alert the user
                notifyUser(entry.note);
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "close to " + a.toString());

                // disable it
                entry.enabled = false;
                final Uri uri =
                    ContentUris.withAppendedId(ReminderProvider.CONTENT_URI,
                                               entry.id);
                final ContentValues values =
                    ReminderProvider.packEntryToValues(entry);
                getContentResolver().update(uri, values, null, null);
                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG, "entry " + entry.id + " is disabled");

                break;
            }
        }
    }

    private boolean inRange(Address current, Address test) {
        /*
         * Test order:
         *   admin area
         *   sub admin area
         *   locality
         *   longitude/latitude
         */
        final String curAdmin = current.getAdminArea();
        final String curSubAdmin = current.getSubAdminArea();
        final String curLocality = current.getLocality();
        final String curThoroughfare = current.getThoroughfare();

        final String admin = test.getAdminArea();
        final String subAdmin = test.getSubAdminArea();
        final String locality = test.getLocality();
        final String thoroughfare = test.getThoroughfare();

        if (test.hasLatitude() && test.hasLongitude()
            && current.hasLatitude() && current.hasLongitude()) {
                float[] distance = new float[1];
                Location.distanceBetween(current.getLatitude(),
                                         current.getLongitude(),
                                         test.getLatitude(),
                                         test.getLongitude(),
                                         distance);
                if (distance[0] <= RANGE) {
                    if (Log.isLoggable(TAG, Log.VERBOSE))
                        Log.v(TAG, "coordinates close to " + test.toString());
                    return true;
                }
        }
        if (admin == null || curAdmin == null
            || !admin.equals(curAdmin)) {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "admin: " + admin + " != " + curAdmin);
            return false;
        }
        if (subAdmin != null && curSubAdmin != null
            && !subAdmin.equals(curSubAdmin)) {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "subAdmin: " + subAdmin + " != " + curSubAdmin);
            return false;
        }
        if (locality != null && curLocality != null
            && !locality.equals(curLocality)) {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "locality: " + locality + " != " + curLocality);
            return false;
        }
        if (thoroughfare != null && curThoroughfare != null
            && !thoroughfare.equals(curThoroughfare)) {
            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.v(TAG, "thoroughfare: " + thoroughfare + " != "
                      + curThoroughfare);
            return false;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "address close to " + test.toString());
        return true;
    }

    private void notifyUser(String message) {
        final String ns = Context.NOTIFICATION_SERVICE;
        final NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(ns);
        final int icon = android.R.drawable.alert_dark_frame;
        final long when = System.currentTimeMillis();

        final Notification notification = new Notification(icon, message, when);

        final Context context = getApplicationContext();
        final CharSequence contentTitle = "Location Alert";
        final Intent notificationIntent = new Intent(this, ReminderList.class);
        final PendingIntent contentIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.setLatestEventInfo(context, contentTitle,
                                        message, contentIntent);
        mNotificationManager.notify(1, notification);
    }
}
