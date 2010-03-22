package shi.ning.locrem;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public final class ProximityManager extends Service {
    private final static int MIN_TIME = 300000; // 5 minutes
    private final static int MIN_DISTANCE = 200; // 200 meters
    private final static int RANGE = 500; // 500 meters

    private Context mContext;
    private Geocoder mGeocoder;
    private LocationManager mManager;
    private ProximityListener mListener;
    private ReminderEntries mEntries;

    private final class ProximityListener implements LocationListener {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // XXX I'm not sure if I care about this
        }

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {
            unregister();
            register();
        }

        @Override
        public void onLocationChanged(Location location) {
            final Address currentAddress = locationToAddress(location);
            final Time now = new Time();
            now.setToNow();
            final LinkedList<ReminderEntry> entries = mEntries.getEnabledAsEntry();

            if (currentAddress == null || entries == null)
                return;

            for (ReminderEntry entry : entries) {
                if (entry.time.after(now))
                    continue;

                for (Address a : entry.addresses) {
                    if (inRange(currentAddress, a)) {
                        // Alert the user, ask if want to disable it, should block
                        notifyUser(entry.note);
                        Log.v(ReminderList.TAG, "in range to " + a.toString());
                    }
                }
            }
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
        mEntries = new ReminderEntries(mContext);
        mEntries.open();
        register();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: maybe we should check for redelivery
        return START_STICKY;
    }

    public Address locationToAddress(Location location) {
        try {
            if (location == null)
                return null;
            final List<Address> addresses =
                mGeocoder.getFromLocation(location.getLatitude(),
                                          location.getLongitude(),
                                          1);
            if (addresses != null && addresses.size() == 1)
                return addresses.get(0);
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private void unregister() {
        mManager.removeUpdates(mListener);
    }

    private void register() {
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        final String provider = mManager.getBestProvider(criteria, true);
        /*
         * TODO Should ask user if they want to enable location service
         * if no provider is available.
         */
        if (provider != null)
            mManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE,
                                            mListener);
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
                if (distance[0] <= RANGE)
                    return true;
        }
        if (admin == null || curAdmin == null
            || !admin.equals(curAdmin))
            return false;
        if (subAdmin != null && curSubAdmin != null
            && !subAdmin.equals(curSubAdmin))
            return false;
        if (locality != null && curLocality != null
            && !locality.equals(curLocality))
            return false;
        if (thoroughfare != null && curThoroughfare != null
            && !thoroughfare.equals(curThoroughfare))
            return false;

        return true;
    }

    private void notifyUser(String message) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        int icon = android.R.drawable.alert_dark_frame;
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, message, when);

        Context context = getApplicationContext();
        CharSequence contentTitle = "Location Alert";
        Intent notificationIntent = new Intent(this, ReminderList.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.setLatestEventInfo(context, contentTitle, message, contentIntent);
        mNotificationManager.notify(1, notification);
    }
}
