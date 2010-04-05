package shi.ning.locrem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public final class EditLocation extends MapActivity {
    static final String TAG = "EditLocation";
    private static final int DIALOG_NONE = -1;
    private static final int DIALOG_NETWORK_UNAVAILABLE = 0;
    private static final int DIALOG_NOT_FOUND = 1;

    Geocoder mGeo;
    private MapView mMapView;
    private MapController mMapController;
    private List<Overlay> mMapOverlays;
    AutoCompleteTextView mLocation;
    private LocationOverlay mItemizedOverlay;
    private AlertDialog.Builder mAlertBuilder;
    List<Address> mAddresses;

    private static final class LocationOverlay
    extends ItemizedOverlay<OverlayItem> {
        private final ArrayList<OverlayItem> mItems;

        public LocationOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));

            mItems = new ArrayList<OverlayItem>();
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
        @Override
        protected Integer doInBackground(Object... params) {
            final boolean isAddress = (Boolean) params[0];

            synchronized (EditLocation.this) {
                try {
                    if (isAddress)
                        mAddresses = mGeo.getFromLocationName((String) params[1],
                                                              1);
                    else
                        mAddresses = mGeo.getFromLocation((Double) params[1],
                                                          (Double) params[2],
                                                          1);
                } catch (IOException e) {
                    return DIALOG_NETWORK_UNAVAILABLE;
                }

                if (mAddresses == null || mAddresses.size() == 0) {
                    return DIALOG_NOT_FOUND;
                }
            }

            if (!isAddress)
                publishProgress((Void) null);

            return DIALOG_NONE;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            updateAddress(mLocation);
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
            case DIALOG_NONE:
                // Save address in recent
                final ContentValues values = new ContentValues();
                values.put(ReminderProvider.RecentColumns.ADDRESS,
                           mLocation.getText().toString());
                getContentResolver().insert(ReminderProvider.RECENT_URI, values);

                updateMap(true);
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

        mAlertBuilder = new AlertDialog.Builder(this);

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
                ime.hideSoftInputFromWindow(arg1.getWindowToken(), 0);
                new GeocodeTask().execute(true, address);
            }
        });
        mLocation.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_SEARCH)
                    return false;
                ime.hideSoftInputFromWindow(v.getWindowToken(), 0);
                new GeocodeTask().execute(true, v.getText().toString());
                return true;
            }
        });

        // Tap overlay
        final Overlay tapOverlay = new Overlay() {
            @Override
            public boolean onTap(GeoPoint p, MapView mapView) {
                new GeocodeTask().execute(false, p.getLatitudeE6() / 1E6,
                                          p.getLongitudeE6() / 1E6);
                return true;
            }
        };
        mMapOverlays.add(tapOverlay);

        final Button save = (Button) findViewById(R.id.save_location);
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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

        Bundle extras = state;
        if (extras == null)
            extras = getIntent().getExtras();

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

        // Try to get the recent entries
        final Cursor c = managedQuery(ReminderProvider.RECENT_URI,
                                      null, null, null, null);
        final String[] from =
            new String[] {ReminderProvider.RecentColumns.ADDRESS};
        final int[] to = new int[] {R.id.recent_address};
        final SimpleCursorAdapter recent =
            new SimpleCursorAdapter(this, R.layout.recent_address_item,
                                    c, from, to);
        recent.setFilterQueryProvider(new RecentFilter(getContentResolver()));
        recent.setCursorToStringConverter(new RecentCursorToString());
        mLocation.setAdapter(recent);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Resources resources = getResources();
        int messageId;

        switch (id) {
        case DIALOG_NETWORK_UNAVAILABLE:
            messageId = R.string.network_unavailable;
            break;
        case DIALOG_NOT_FOUND:
            messageId = R.string.address_not_found;
            break;
        default:
            return null;
        }

        mAlertBuilder.setMessage(resources.getString(messageId))
                     .setCancelable(false)
                     .setPositiveButton(resources.getString(R.string.ok), null);
        return mAlertBuilder.create();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    void updateAddress(EditText location) {
        if (mAddresses.isEmpty())
            return;

        final Address address = mAddresses.get(0);
        final int len = mAddresses.get(0).getMaxAddressLineIndex();
        if (len == -1)
            return;

        final StringBuilder addressLine =
            new StringBuilder(address.getAddressLine(0));
        for (int i = 1; i < len; i++) {
            addressLine.append(", ");
            addressLine.append(address.getAddressLine(i));
        }
        location.setText(addressLine);
    }

    void updateMap(boolean zoom) {
        mItemizedOverlay.clear();
        final int length = mAddresses.size();
        for (int i = 0; i < length; i++) {
            final Address a = mAddresses.get(i);
            final GeoPoint point = new GeoPoint((int) (a.getLatitude() * 1E6),
                                                (int) (a.getLongitude() * 1E6));
            final OverlayItem item = new OverlayItem(point, "", "");
            mItemizedOverlay.addItem(item);
        }
        mMapOverlays.add(mItemizedOverlay);
        mMapController.animateTo(mItemizedOverlay.getCenter());
        if (zoom)
            mMapController.setZoom(16);
    }
}
