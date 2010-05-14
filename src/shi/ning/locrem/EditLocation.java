package shi.ning.locrem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public final class EditLocation extends MapActivity
implements ServiceConnection {
    static final String TAG = "EditLocation";
    private static final int SHOW_CURRENT = -2;
    private static final int DIALOG_NONE = -1;
    private static final int DIALOG_NETWORK_UNAVAILABLE = 0;
    private static final int DIALOG_NOT_FOUND = 1;
    private static final int DIALOG_INVALID_ADDRESS = 2;
    private static final int DIALOG_CHANGE_ADDRESS = 3;
    private static final int DIALOG_FINDING_LOCATION = 4;
    private static final int DIALOG_FINDING_CURRENT_LOCATION = 5;

    static final int LONG_TAP_DURATION = 1500; // 2 seconds
    static final double MAX_LONG_TAP_DISTANCE = 100.0;

    Geocoder mGeo;
    private MapView mMapView;
    private MapController mMapController;
    private List<Overlay> mMapOverlays;
    AutoCompleteTextView mLocation;
    private LocationOverlay mItemizedOverlay;
    private ProximityManagerService mPMService;
    private int mOpenDialog;
    List<Address> mAddresses;
    List<Address> mTmpAddresses;
    boolean mIsLongTap;
    GeoPoint mTapLocation;
    boolean mIsRunning;

    private final class LocationOverlay
    extends ItemizedOverlay<OverlayItem> {
        private final ArrayList<OverlayItem> mItems;
        private final Paint mPaintBorder;
        private final Paint mPaintBody;

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow) {
                Projection projection = mapView.getProjection();
                SharedPreferences pref =
                    PreferenceManager.getDefaultSharedPreferences
                    (EditLocation.this.getApplication());
                int radius =
                    Integer.parseInt(pref.getString(Settings.KEY_RANGE,
                                                    Settings.DEFAULT_RANGE));
                float radiusOnScreen = projection.metersToEquatorPixels(radius);
                Point out = new Point();
                int len = mItems.size();

                for (int i = 0; i < len; i++) {
                    projection.toPixels(mItems.get(i).getPoint(), out);
                    canvas.drawCircle(out.x, out.y, radiusOnScreen, mPaintBorder);
                    canvas.drawCircle(out.x, out.y, radiusOnScreen, mPaintBody);
                }
            }

            super.draw(canvas, mapView, shadow);
        }

        public LocationOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));

            mItems = new ArrayList<OverlayItem>();
            mPaintBorder = new Paint();
            mPaintBorder.setARGB(100, 0, 0, 255);
            mPaintBorder.setStyle(Paint.Style.STROKE);
            mPaintBorder.setAntiAlias(true);
            mPaintBody = new Paint();
            mPaintBody.setARGB(30, 0, 0, 255);
            mPaintBody.setAntiAlias(true);
        }

        @Override
        protected OverlayItem createItem(int i) {
            return mItems.get(i);
        }

        @Override
        public int size() {
            return mItems.size();
        }

        public void addItem(OverlayItem item) {
            mItems.add(item);
            populate();
        }

        public void clear() {
            mItems.clear();
        }
    }

    private final class GeocodeTask extends AsyncTask<Object, Void, Integer> {
        public static final int TYPE_ADDRESS = 0;
        public static final int TYPE_COORDINATES = 1;
        public static final int TYPE_CURRENT = 2;

        private final Resources mResources;
        private final int mType;
        private Address mCurrent;

        public GeocodeTask(int type) {
            mResources = getResources();
            mType = type;
            mCurrent = null;
            mTmpAddresses = null;
        }

        @Override
        protected Integer doInBackground(Object... params) {
            int retval = DIALOG_NONE;

            /*
             * No need to synchronize for mTmpAddresses, since we will never
             * access it concurrently in two threads.
             */
            try {
                switch (mType) {
                case TYPE_ADDRESS:
                    mTmpAddresses = mGeo.getFromLocationName((String) params[0],
                                                             1);
                    break;
                case TYPE_COORDINATES:
                    mTmpAddresses = mGeo.getFromLocation((Double) params[0],
                                                         (Double) params[1],
                                                         1);
                    retval = DIALOG_CHANGE_ADDRESS;
                    break;
                case TYPE_CURRENT:
                    // Let's spin until mPMService is ready
                    int count = 5;
                    while (count-- > 0) {
                        synchronized (EditLocation.this) {
                            if (mPMService != null)
                                break;
                        }
                        Thread.sleep(500);
                    }

                    synchronized (EditLocation.this) {
                        if (mPMService != null) {
                            if (Log.isLoggable(TAG, Log.VERBOSE))
                                Log.v(TAG, "finding current location");

                            try {
                                final byte[] currentLocation =
                                    mPMService.getCurrentLocation();
                                mCurrent =
                                    ReminderEntry.deserializeAddress(currentLocation);
                            } catch (RemoteException e) {}
                        }
                    }
                    return SHOW_CURRENT;
                }
            } catch (IOException e) {
                return DIALOG_NETWORK_UNAVAILABLE;
            } catch (InterruptedException e) {}

            if (mTmpAddresses == null || mTmpAddresses.size() == 0) {
                return DIALOG_NOT_FOUND;
            }

            return retval;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mOpenDialog >= 0) {
                dismissDialog(mOpenDialog);
                mOpenDialog = -1;
            }

            // In case the activity ended earlier
            if (mIsRunning == false)
                return;

            switch (result) {
            case DIALOG_NONE:
                mAddresses = mTmpAddresses;
                mTmpAddresses = null;
                saveAddressToRecent();
                updateMap(true);
                break;
            case SHOW_CURRENT:
                if (mCurrent != null) {
                    if (Log.isLoggable(TAG, Log.VERBOSE))
                        Log.v(TAG, "showing current location");
                    centerMap(addressToGeoPoint(mCurrent), true);
                } else {
                    final String message =
                        mResources.getString(R.string.current_location_not_found);
                    EditLocation.this.notify(message, Toast.LENGTH_SHORT);
                }
                break;
            default:
                showDialog(result);
            }
        }
    }

    private static final class RecentFilter implements FilterQueryProvider {
        private final ContentResolver mResolver;

        public RecentFilter(ContentResolver c) {
            mResolver = c;
        }

        @Override
        public Cursor runQuery(CharSequence constraint) {
            final String selection = (ReminderProvider.RecentColumns.ADDRESS
                    + " LIKE '" + constraint + "%'");
            return mResolver.query(ReminderProvider.RECENT_URI, null,
                                   selection, null, null);
        }
    }

    private static final class RecentCursorToString
    implements SimpleCursorAdapter.CursorToStringConverter {
        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(ReminderProvider.RecentColumns.ADDRESS_INDEX);
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.map);

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);

        mMapController = mMapView.getController();
        mMapOverlays = mMapView.getOverlays();
        final Drawable marker =
            getResources().getDrawable(R.drawable.red_circle);
        mItemizedOverlay = new LocationOverlay(marker);

        final InputMethodManager ime =
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mLocation = (AutoCompleteTextView) findViewById(R.id.edit_location);
        mGeo = new Geocoder(getApplicationContext());
        mLocation.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int arg2, long arg3) {
                final String address = ((TextView) arg1).getText().toString();
                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG, "selected from drop down menu: " + address);
                ime.hideSoftInputFromWindow(arg1.getApplicationWindowToken(),
                                            0);
                mOpenDialog = DIALOG_FINDING_LOCATION;
                showDialog(mOpenDialog);
                new GeocodeTask(GeocodeTask.TYPE_ADDRESS).execute(address);
            }
        });
        mLocation.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_SEARCH)
                    return false;
                ime.hideSoftInputFromWindow(v.getWindowToken(), 0);
                ((AutoCompleteTextView) v).dismissDropDown();
                mOpenDialog = DIALOG_FINDING_LOCATION;
                showDialog(mOpenDialog);
                new GeocodeTask(GeocodeTask.TYPE_ADDRESS)
                        .execute(v.getText().toString());
                return true;
            }
        });

        final Runnable longTapRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsLongTap && mTapLocation != null) {
                    mOpenDialog = DIALOG_FINDING_LOCATION;
                    showDialog(mOpenDialog);
                    new GeocodeTask(GeocodeTask.TYPE_COORDINATES)
                            .execute(mTapLocation.getLatitudeE6() / 1E6,
                                     mTapLocation.getLongitudeE6() / 1E6);
                }
            }
        };

        // Tap overlay
        mIsLongTap = false;
        mTapLocation = null;
        final Overlay tapOverlay = new Overlay() {
            private float firstX;
            private float firstY;

            @Override
            public boolean onTouchEvent(MotionEvent e, MapView mapView) {
                final int actionId = e.getAction();

                if (actionId == MotionEvent.ACTION_DOWN) {
                    mIsLongTap = true;
                    firstX = e.getX();
                    firstY = e.getY();
                    mTapLocation = mapView.getProjection()
                                          .fromPixels((int) firstX,
                                                      (int) firstY);
                    mapView.postDelayed(longTapRunnable, LONG_TAP_DURATION);
                } else if ((actionId & MotionEvent.ACTION_POINTER_DOWN)
                            == MotionEvent.ACTION_POINTER_DOWN
                            || actionId == MotionEvent.ACTION_UP) {
                    mIsLongTap = false;
                    mTapLocation = null;
                } else if (actionId == MotionEvent.ACTION_MOVE) {
                    final double dist = Math.pow(e.getX() - firstX, 2)
                                        + Math.pow(e.getY() - firstY, 2);
                    if (dist > MAX_LONG_TAP_DISTANCE) {
                        mIsLongTap = false;
                        mTapLocation = null;
                    }
                }

                return false;
            }
        };
        mMapOverlays.add(tapOverlay);

        final Button save = (Button) findViewById(R.id.save_location);
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkForm()) {
                    showDialog(DIALOG_INVALID_ADDRESS);
                    return;
                }

                Intent intent = new Intent();
                intent.putExtra(ReminderEntry.Columns.LOCATION,
                                mLocation.getText().toString());
                synchronized (EditLocation.this) {
                    intent.putExtra(ReminderEntry.Columns.ADDRESSES,
                                    ReminderEntry.serializeAddresses(mAddresses));
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        final Intent intent = new Intent(this, ProximityManager.class);
        bindService(intent, this, 0);

        Bundle extras = state;
        if (extras == null)
            extras = getIntent().getExtras();

        if (extras != null) {
            final String locationString =
                extras.getString(ReminderEntry.Columns.LOCATION);
            if (locationString != null)
                mLocation.setText(locationString);
            final byte[] buffer =
                extras.getByteArray(ReminderEntry.Columns.ADDRESSES);
            if (buffer != null) {
                mAddresses = ReminderEntry.deserializeAddresses(buffer);
                if (mAddresses != null)
                    updateMap(true);
            }
        }

        if (mAddresses == null) {
            mOpenDialog = DIALOG_FINDING_CURRENT_LOCATION;
            showDialog(mOpenDialog);
            // Set to the current location
            new GeocodeTask(GeocodeTask.TYPE_CURRENT).execute();
        }

        // Try to get the recent entries
        final Cursor c = managedQuery(ReminderProvider.RECENT_URI,
                                      null, null, null, null);
        final String[] from =
            new String[] {ReminderProvider.RecentColumns.ADDRESS};
        final int[] to = new int[] {R.id.auto_complete_entry};
        final SimpleCursorAdapter recent =
            new SimpleCursorAdapter(this, R.layout.auto_complete_item,
                                    c, from, to);
        recent.setFilterQueryProvider(new RecentFilter(getContentResolver()));
        recent.setCursorToStringConverter(new RecentCursorToString());
        mLocation.setAdapter(recent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsRunning = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_center_to_current:
            mOpenDialog = DIALOG_FINDING_CURRENT_LOCATION;
            showDialog(mOpenDialog);
            new GeocodeTask(GeocodeTask.TYPE_CURRENT).execute();
            return true;
        case R.id.menu_switch_mode:
            mMapView.setSatellite(!mMapView.isSatellite());
            return true;
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ProgressDialog dialog = new ProgressDialog(this);
        final Resources resources = getResources();
        int title = R.string.title_error;
        String message = "";
        boolean cancelable = false;
        DialogInterface.OnClickListener okListener = null;

        switch (id) {
        case DIALOG_NETWORK_UNAVAILABLE:
            message = resources.getString(R.string.network_unavailable);
            break;
        case DIALOG_NOT_FOUND:
            message = resources.getString(R.string.address_not_found);
            break;
        case DIALOG_INVALID_ADDRESS:
            message = resources.getString(R.string.address_invalid);
            break;
        case DIALOG_CHANGE_ADDRESS:
            if (mTmpAddresses != null && mTmpAddresses.size() > 0) {
                title = R.string.title_alert;
                cancelable = true;
                okListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAddresses = mTmpAddresses;
                        mTmpAddresses = null;
                        updateAddress(mLocation);
                        saveAddressToRecent();
                        updateMap(false);
                    }
                };
            } else {
                message = resources.getString(R.string.address_not_found);
            }

            break;
        case DIALOG_FINDING_LOCATION:
            message = resources.getString(R.string.finding_location);
            dialog.setMessage(message);
            return dialog;
        case DIALOG_FINDING_CURRENT_LOCATION:
            message = resources.getString(R.string.finding_current_location);
            dialog.setMessage(message);
            return dialog;
        default:
            return null;
        }

        builder.setTitle(title)
               .setMessage(message)
               .setCancelable(cancelable)
               .setPositiveButton(R.string.ok, okListener);
        if (cancelable) {
            builder.setNegativeButton(R.string.cancel,
                                      new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mTmpAddresses = null;
                    dialog.cancel();
                }
            });
        }
        return builder.create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        final Resources resources = getResources();
        String message = null;

        switch (id) {
        case DIALOG_CHANGE_ADDRESS:
            if (mTmpAddresses != null && !mTmpAddresses.isEmpty())
                message = resources.getString(R.string.address_change,
                                              addressToString(mTmpAddresses.get(0)));
            break;
        }

        if (message != null)
            ((AlertDialog) dialog).setMessage(message);
        super.onPrepareDialog(id, dialog);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    String addressToString(Address address) {
        final int len = address.getMaxAddressLineIndex();
        if (len == -1)
            return null;

        final StringBuilder addressLine =
            new StringBuilder(address.getAddressLine(0));
        for (int i = 1; i < len; i++) {
            addressLine.append(", ");
            addressLine.append(address.getAddressLine(i));
        }

        return addressLine.toString();
    }

    void updateAddress(EditText location) {
        if (mAddresses == null || mAddresses.isEmpty())
            return;

        final Address address = mAddresses.get(0);
        final CharSequence locationString = addressToString(address);
        if (locationString != null)
            location.setText(locationString);
    }

    void saveAddressToRecent() {
        // Save address in recent
        final ContentValues values = new ContentValues();
        values.put(ReminderProvider.RecentColumns.ADDRESS,
                   mLocation.getText().toString());
        getContentResolver().insert(ReminderProvider.RECENT_URI,
                                    values);
    }

    void updateMap(boolean zoom) {
        mItemizedOverlay.clear();
        final int length = mAddresses.size();
        for (int i = 0; i < length; i++) {
            final Address a = mAddresses.get(i);
            final GeoPoint point = addressToGeoPoint(a);
            final OverlayItem item = new OverlayItem(point, "", "");
            mItemizedOverlay.addItem(item);
        }
        if (!mMapOverlays.contains(mItemizedOverlay))
            mMapOverlays.add(mItemizedOverlay);
        centerMap(mItemizedOverlay.getCenter(), zoom);
    }

    void centerMap(GeoPoint center, boolean zoom) {
        mMapController.animateTo(center);
        if (zoom)
            mMapController.setZoom(16);
    }

    GeoPoint addressToGeoPoint(final Address a) {
        return new GeoPoint((int) (a.getLatitude() * 1E6),
                            (int) (a.getLongitude() * 1E6));
    }

    private boolean checkForm() {
        synchronized (this) {
            if (mLocation.getText().length() == 0
                || mAddresses == null || mAddresses.size() == 0)
                return false;
            return true;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this) {
            mPMService = ProximityManagerService.Stub.asInterface(service);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this) {
            mPMService = null;
        }
    }

    void notify(String message, int duration) {
        Context context = getApplicationContext();
        Toast.makeText(context, message, duration).show();
    }
}
