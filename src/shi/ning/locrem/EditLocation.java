package shi.ning.locrem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public final class EditLocation extends MapActivity {
    private static final int DIALOG_NETWORK_UNAVAILABLE = 0;
    private static final int DIALOG_NOT_FOUND = 1;

    private MapController mMapController;
    private List<Overlay> mMapOverlays;
    private LocationOverlay mItemizedOverlay;
    private AlertDialog.Builder mAlertBuilder;
    private List<Address> mAddresses;

    private final class LocationOverlay extends ItemizedOverlay<OverlayItem> {
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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.map);

        final MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);

        mMapController = mapView.getController();
        mMapOverlays = mapView.getOverlays();
        final Drawable marker = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
        mItemizedOverlay = new LocationOverlay(marker);

        mAlertBuilder = new AlertDialog.Builder(this);

        final InputMethodManager ime = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        final EditText location = (EditText) findViewById(R.id.edit_location);
        location.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_SEARCH)
                    return false;
                ime.hideSoftInputFromWindow(v.getWindowToken(), 0);

                final Geocoder geo = new Geocoder(getApplicationContext());
                mItemizedOverlay.clear();
                mMapOverlays.clear();
                try {
                    mAddresses = geo.getFromLocationName(v.getText().toString(), 1);
                } catch (IOException e) {
                    mapView.postInvalidate();
                    showDialog(DIALOG_NETWORK_UNAVAILABLE);
                    return true;
                }

                if (mAddresses == null || mAddresses.size() == 0) {
                    mapView.postInvalidate();
                    showDialog(DIALOG_NOT_FOUND);
                    return true;
                }

                updateMap();

                return true;
            }
        });

        final Button save = (Button) findViewById(R.id.save_location);
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(ReminderEntry.Columns.LOCATION,
                                location.getText().toString());
                intent.putExtra(ReminderEntry.Columns.ADDRESSES,
                                ReminderEntry.serializeAddresses(mAddresses));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Bundle extras = state;
        if (extras == null)
            extras = getIntent().getExtras();

        final String locationString = extras.getString(ReminderEntry.Columns.LOCATION);
        if (locationString != null)
            location.setText(locationString);
        final byte[] buffer = extras.getByteArray(ReminderEntry.Columns.ADDRESSES);
        if (buffer != null) {
            mAddresses = ReminderEntry.deserializeAddresses(buffer);
            if (mAddresses != null)
                updateMap();
        }
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

    private void updateMap() {
        for (Address a : mAddresses) {
            final GeoPoint point = new GeoPoint((int) (a.getLatitude() * 1E6),
                                                (int) (a.getLongitude() * 1E6));
            final OverlayItem item = new OverlayItem(point, "", "");
            mItemizedOverlay.addItem(item);
        }
        mMapOverlays.add(mItemizedOverlay);
        mMapController.setCenter(mItemizedOverlay.getCenter());
        mMapController.setZoom(16);
    }
}
