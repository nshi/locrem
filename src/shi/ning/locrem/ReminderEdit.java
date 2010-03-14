package shi.ning.locrem;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

public final class ReminderEdit extends Activity {
    private static final int ACTIVITY_LOCATION = 0;
    private static final int DIALOG_DATE = 1;
    private static final int DIALOG_TIME = 2;

    private ReminderEntries mEntries;
    private long mId;
    private ReminderEntry mEntry;

    private TextView mLocationLabel;
    private TextView mDateLabel;
    private TextView mTimeLabel;
    private EditText mNote;

    private final OnDateSetListener mDateSetListener =
        new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view,
                                  int year, int monthOfYear, int dayOfMonth) {
                mEntry.time.year = year;
                mEntry.time.month = monthOfYear;
                mEntry.time.monthDay = dayOfMonth;
                updateDateLabel();
            }
        };
    private final OnTimeSetListener mTimeSetListener =
        new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mEntry.time.hour = hourOfDay;
                mEntry.time.minute = minute;
                updateTimeLabel();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        View setLocation = findViewById(R.id.set_location);
        setLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocation();
            }
        });

        mDateLabel = (TextView) findViewById(R.id.date_label);
        mDateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_DATE);
            }
        });

        mTimeLabel = (TextView) findViewById(R.id.time_label);
        mTimeLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_TIME);
            }
        });

        mLocationLabel = (TextView) findViewById(R.id.location_label);
        mNote = (EditText) findViewById(R.id.note);

        Button save = (Button) findViewById(R.id.save);
        Button cancel = (Button) findViewById(R.id.cancel);

        mEntries = new ReminderEntries(this);
        mEntries.open();
        mId = -1;
        if (savedInstanceState != null)
            mId = savedInstanceState.getLong(ReminderEntry.Columns._ID);
        else
            mId = getIntent().getLongExtra(ReminderEntry.Columns._ID, -1);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(ReminderEntry.Columns._ID, mId);
                saveEntry();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_DATE:
            return new DatePickerDialog(this,
                                        mDateSetListener,
                                        mEntry.time.year,
                                        mEntry.time.month,
                                        mEntry.time.monthDay);
        case DIALOG_TIME:
            return new TimePickerDialog(this,
                                        mTimeSetListener,
                                        mEntry.time.hour,
                                        mEntry.time.minute,
                                        false);
        }

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        populateFields();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case ACTIVITY_LOCATION:
            if (resultCode == RESULT_OK) {
                // TODO unpack location data
                mEntry.location = data.getStringExtra(ReminderEntry.Columns.LOCATION);
                mEntry.addresses = ReminderEntry.deserializeAddresses(data.getByteArrayExtra(ReminderEntry.Columns.ADDRESSES));
                updateLocationLabel();
            }
            break;
        }
    }

    private void populateFields() {
        // TODO Check if there is any unsaved data, if so, load it from the
        // temporary table, otherwise wipe the slate clean.
        if (mId >= 0)
            mEntry = mEntries.getEntry(mId);

        if (mEntry != null)
            mNote.setText(mEntry.note);
        else
            mEntry = new ReminderEntry("", "", null);

        updateLocationLabel();
        updateDateLabel();
        updateTimeLabel();
    }

    @Override
    protected void onPause() {
        // TODO Dump unsaved data to a temporary table
        super.onPause();

        mEntries.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ReminderEntry.Columns._ID, mId);
    }

    private void setLocation() {
        Intent i = new Intent(this, EditLocation.class);
        if (mEntry.location != null)
            i.putExtra(ReminderEntry.Columns.LOCATION, mEntry.location);
        if (mEntry.addresses != null)
            i.putExtra(ReminderEntry.Columns.ADDRESSES,
                       ReminderEntry.serializeAddresses(mEntry.addresses));
        startActivityForResult(i, ACTIVITY_LOCATION);
    }

    private void saveEntry() {
        if (mId >= 0)
            mEntries.updateEntry(mEntry);
        else
            mEntries.createEntry(mEntry);
    }

    private void updateLocationLabel() {
        if (mEntry.location.length() > 0)
            mLocationLabel.setText(mEntry.location);
        else
            mLocationLabel.setText(R.string.location);
    }

    private void updateDateLabel() {
        mDateLabel.setText(mEntry.time.format("%a, %b %e"));
    }

    private void updateTimeLabel() {
        mTimeLabel.setText(mEntry.time.format("%r"));
    }
}
