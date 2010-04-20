package shi.ning.locrem;

import shi.ning.locrem.ReminderEntry.Columns;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public final class ReminderEdit extends Activity {
    static final String TAG = "ReminderEdit";
    private static final int ACTIVITY_LOCATION = 0;
    private static final int DIALOG_DATE = 1;
    private static final int DIALOG_TIME = 2;
    private static final int DIALOG_INCOMPLETE_FORM = 3;

    private long mId;
    ReminderEntry mEntry;

    private TextView mLocationLabel;
    private TextView mDateLabel;
    private TextView mTimeLabel;
    private AutoCompleteTextView mTag;
    private EditText mNote;
    private AlertDialog.Builder mAlertBuilder;

    private final OnDateSetListener mDateSetListener =
        new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view,
                                  int year, int monthOfYear, int dayOfMonth) {
                mEntry.time.set(0,
                                mEntry.time.minute,
                                mEntry.time.hour,
                                dayOfMonth, monthOfYear, year);
                mEntry.time.normalize(true);
                updateDateLabel();
            }
        };
    private final OnTimeSetListener mTimeSetListener =
        new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mEntry.time.set(0, minute, hourOfDay,
                                mEntry.time.monthDay,
                                mEntry.time.month,
                                mEntry.time.year);
                mEntry.time.normalize(true);
                updateTimeLabel();
            }
        };

    private static final class TagFilter implements FilterQueryProvider {
        private final ContentResolver mResolver;

        public TagFilter(ContentResolver c) {
            mResolver = c;
        }

        @Override
        public Cursor runQuery(CharSequence constraint) {
            String selection = null;
            if (constraint != null)
                selection = (ReminderEntry.Columns.TAG + " LIKE '"
                             + constraint + "%'");
            return mResolver.query(ReminderProvider.TAGS_URI, null,
                                   selection, null, null);
        }
    }

    private static final class TagCursorToString
    implements SimpleCursorAdapter.CursorToStringConverter {
        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(ReminderEntry.Columns.TAG_INDEX);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mLocationLabel = (TextView) findViewById(R.id.set_location);
        mLocationLabel.setOnClickListener(new View.OnClickListener() {
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

        mTag = (AutoCompleteTextView) findViewById(R.id.edit_tag);
        mNote = (EditText) findViewById(R.id.note);

        final Button save = (Button) findViewById(R.id.save);
        final Button cancel = (Button) findViewById(R.id.cancel);

        mId = -1;
        if (savedInstanceState != null)
            mId = savedInstanceState.getLong(Columns._ID);
        else
            mId = getIntent().getLongExtra(Columns._ID, -1);

        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkForm()) {
                    showDialog(DIALOG_INCOMPLETE_FORM);
                    return;
                }

                Intent intent = new Intent();
                intent.putExtra(Columns._ID, mId);
                saveEntry();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mAlertBuilder = new AlertDialog.Builder(this);

        // Set auto complete for the tags
        final Cursor c = managedQuery(ReminderProvider.TAGS_URI,
                                      null, null, null, null);
        final String[] from =
            new String[] {ReminderEntry.Columns.TAG};
        final int[] to = new int[] {R.id.auto_complete_entry};
        final SimpleCursorAdapter tags =
            new SimpleCursorAdapter(this, R.layout.auto_complete_item,
                                    c, from, to);
        tags.setFilterQueryProvider(new TagFilter(getContentResolver()));
        tags.setCursorToStringConverter(new TagCursorToString());
        mTag.setAdapter(tags);

        populateFields();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Resources resources = getResources();
        int messageId = 0;

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
        case DIALOG_INCOMPLETE_FORM:
            messageId = R.string.incomplete_form;
            break;
        }

        mAlertBuilder.setMessage(resources.getText(messageId))
                     .setCancelable(false)
                     .setPositiveButton(resources.getText(R.string.ok), null);
        return mAlertBuilder.create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case ACTIVITY_LOCATION:
            if (resultCode == RESULT_OK) {
                final byte[] blob = data.getByteArrayExtra(Columns.ADDRESSES);
                mEntry.location = data.getStringExtra(Columns.LOCATION);
                mEntry.addresses = ReminderEntry.deserializeAddresses(blob);
                updateLocationLabel();
            }
            break;
        }
    }

    private void populateFields() {
        if (mEntry == null && mId >= 0) {
            final Uri uri =
                ContentUris.withAppendedId(ReminderProvider.CONTENT_URI, mId);
            final Cursor cursor = managedQuery(uri, null, null, null, null);
            if (cursor.moveToFirst())
                mEntry = ReminderProvider.cursorToEntry(cursor);
        }

        if (mEntry == null)
            mEntry = new ReminderEntry("", "", null);

        mTag.setText(mEntry.tag);
        mNote.setText(mEntry.note);
        updateLocationLabel();
        updateDateLabel();
        updateTimeLabel();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Columns._ID, mId);
    }

    void setLocation() {
        Intent i = new Intent(this, EditLocation.class);
        if (mEntry.location != null)
            i.putExtra(Columns.LOCATION, mEntry.location);
        if (mEntry.addresses != null)
            i.putExtra(Columns.ADDRESSES,
                       ReminderEntry.serializeAddresses(mEntry.addresses));
        startActivityForResult(i, ACTIVITY_LOCATION);
    }

    void saveEntry() {
        final Resources resources = getResources();
        mEntry.tag = mTag.getText().toString();
        mEntry.note = mNote.getText().toString();
        final ContentValues values = ReminderProvider.packEntryToValues(mEntry);
        if (mId >= 0) {
            Uri uri = ContentUris.withAppendedId(ReminderProvider.CONTENT_URI,
                                                 mEntry.id);
            if (getContentResolver().update(uri, values, null, null) != 1) {
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "failed to save entry " + mEntry.id);
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE))
                    Log.d(TAG, "successfully updated entry " + mEntry.id);
            }
        } else {
            final Uri uri =
                getContentResolver().insert(ReminderProvider.CONTENT_URI,
                                            values);

            if (Log.isLoggable(TAG, Log.VERBOSE))
                Log.d(TAG, "successfully inserted entry " + uri);
        }

        notify(String.format(resources.getString(R.string.save_succeeded),
                             mEntry.note), Toast.LENGTH_SHORT);
    }

    private void updateLocationLabel() {
        if (mEntry.location.length() > 0)
            mLocationLabel.setText(mEntry.location);
    }

    void updateDateLabel() {
        mDateLabel.setText(mEntry.time.format("%a, %b %e"));
    }

    void updateTimeLabel() {
        mTimeLabel.setText(mEntry.time.format("%I:%M %p"));
    }

    private boolean checkForm() {
        if (mNote.getText().length() == 0
            || mEntry.location == null
            || mEntry.addresses == null
            || mEntry.addresses.size() == 0)
            return false;
        return true;
    }

    private void notify(String message, int duration) {
        Context context = getApplicationContext();
        Toast.makeText(context, message, duration).show();
    }
}
