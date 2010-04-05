package shi.ning.locrem;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public final class Settings extends Activity {
    static final String TAG = "Settings";
    private static final double WALKING_SPEED = 1.66; // 1.66 m/s

    public static final String KEY_RANGE = "range";

    public static final int DEFAULT_RANGE = 500;

    SharedPreferences mSettings;
    private SeekBar mRangeBar;
    TextView mDistanceLabel;
    TextView mWalkingLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        mRangeBar = (SeekBar) findViewById(R.id.range_bar);
        mDistanceLabel = (TextView) findViewById(R.id.settings_meters_label);
        mWalkingLabel = (TextView) findViewById(R.id.walking_time);

        mRangeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            private int mRange;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mSettings.edit().putInt(KEY_RANGE, mRange).commit()) {
                    if (Log.isLoggable(TAG, Log.WARN))
                        Log.w(TAG, "failed to save settings");

                    Settings.this.notify("failed to save settings",
                                         Toast.LENGTH_LONG);
                }

                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG, "succeessfully saved settings");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                final double walkingTime = progress / WALKING_SPEED; // seconds
                final DecimalFormat formatter = new DecimalFormat("#.#");
                mRange = progress;

                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "range changed to " + progress
                          + " by " + (fromUser ? "user" : "program"));

                if (progress < 500)
                    mDistanceLabel.setHint(progress + " m");
                else
                    mDistanceLabel.setHint(formatter.format(progress / 1000.0)
                                           + " km");

                if (walkingTime < 60.0)
                    mWalkingLabel.setHint(formatter.format(walkingTime)
                                          + " secs");
                else
                    mWalkingLabel.setHint(formatter.format(walkingTime / 60.0)
                                          + " mins");
            }
        });

        mSettings =
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "created");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "loading settings");
        mRangeBar.setProgress(mSettings.getInt(KEY_RANGE, DEFAULT_RANGE));
    }

    private void notify(String message, int duration) {
        Context context = getApplicationContext();
        Toast.makeText(context, message, duration).show();
    }
}
