package shi.ning.locrem;

import java.text.DecimalFormat;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public final class Settings extends PreferenceActivity
implements OnSharedPreferenceChangeListener {
    static final String TAG = "Settings";
    private static final double WALKING_SPEED = 1.66; // 1.66 m/s

    public static final String KEY_RANGE = "settings_range";
    public static final String KEY_RINGTONE = "settings_ringtone";
    public static final String KEY_VIBRATION = "settings_vibration";

    public static final String DEFAULT_RANGE = "500";
    public static final boolean DEFAULT_VIBRATION = false;

    private ListPreference mRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.settings);

        getPreferenceScreen().getSharedPreferences()
                             .registerOnSharedPreferenceChangeListener(this);

        mRange = (ListPreference) findPreference(KEY_RANGE);

        final int value =
            Integer.parseInt(mRange.getValue());
        updateRangeSummary(value);

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "created");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref,
                                          String key) {
        if (key.equals(KEY_RANGE)) {
            final int value =
                Integer.parseInt(pref.getString(KEY_RANGE, DEFAULT_RANGE));
            updateRangeSummary(value);
        }
    }

    private void updateRangeSummary(int value) {
        final Resources resources = getResources();
        final DecimalFormat formatter = new DecimalFormat("#.#");
        final double walkingTime = value / WALKING_SPEED;

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "range changed to " + value);

        String distance = null;
        if (value <= 500)
            distance = resources.getString(R.string.distance_in_m, value);
        else
            distance = resources.getString(R.string.distance_in_km,
                                           formatter.format(value / 1000.0));

        String walking = null;
        if (walkingTime < 60.0)
            walking = resources.getString(R.string.time_in_sec,
                                          formatter.format(walkingTime));
        else
            walking = resources.getString(R.string.time_in_min,
                                          formatter.format(walkingTime / 60.0));
        final String summary = distance + " (" + walking + " "
            + resources.getString(R.string.walk) + ")";
        mRange.setSummary(summary);
    }
}
