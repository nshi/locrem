package shi.ning.locrem;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public final class ProximityManager {
    private final static int MIN_TIME = 300000; // 5 minutes
    private final static int MIN_DISTANCE = 200; // 200 meters

    private final Context mContext;
    private final Geocoder mGeocoder;
    private final LocationManager mManager;
    private final ProximityListener mListener;

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
            // TODO Auto-generated method stub
            notifyUser(locationToAddress(location).toString());
        }
    }

    public ProximityManager(Context context) {
        mContext = context;
        mManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mGeocoder = new Geocoder(context);
        mListener = new ProximityListener();
        register();
    }

    public Address locationToAddress(Location location) {
        try {
            if (location == null)
                return null;
            final List<Address> addresses = mGeocoder.getFromLocation(location.getLatitude(),
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

    private void notifyUser(String message) {
        Context context = mContext;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
