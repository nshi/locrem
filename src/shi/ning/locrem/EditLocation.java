package shi.ning.locrem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
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

    private AlertDialog.Builder mAlertBuilder;

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
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.map);

        final MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);

        final MapController mapController = mapView.getController();
        final List<Overlay> mapOverlays = mapView.getOverlays();
        final Drawable marker = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
        final LocationOverlay itemizedOverlay = new LocationOverlay(marker);
        mapOverlays.clear();
        mapOverlays.add(itemizedOverlay);

        mAlertBuilder = new AlertDialog.Builder(this);

        final EditText location = (EditText) findViewById(R.id.edit_location);
        location.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_SEARCH)
                    return false;

                final Geocoder geo = new Geocoder(getApplicationContext());
                List<Address> addrs = null;
                final String addressLine = v.getText().toString();
                itemizedOverlay.clear();
                try {
                    addrs = geo.getFromLocationName(addressLine, 1);
                } catch (IOException e) {
                    showDialog(DIALOG_NETWORK_UNAVAILABLE);
                    return true;
                }

                if (addrs == null || addrs.size() == 0) {
                    showDialog(DIALOG_NOT_FOUND);
                    return true;
                }

                for (Address a : addrs) {
                    final GeoPoint point = new GeoPoint((int) (a.getLatitude() * 1E6),
                                                        (int) (a.getLongitude() * 1E6));
                    final OverlayItem item = new OverlayItem(point, "", addressLine);
                    itemizedOverlay.addItem(item);
                }
                mapController.setCenter(itemizedOverlay.getCenter());
                mapController.setZoom(16);

                return true;
            }
        });
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

}
