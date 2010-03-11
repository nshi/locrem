package shi.ning.locrem;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

public final class ReminderEdit extends Activity {
    private ReminderEntries mEntries;
    private long mId;

    private EditText mLocation;
    private EditText mNote;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mLocation = (EditText) findViewById(R.id.location);
        mNote = (EditText) findViewById(R.id.note);

        mLocation.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_ENTER)
                    return false;

                Geocoder geo = new Geocoder(getApplicationContext());
                List<Address> addrs = null;
                try {
                    addrs = geo.getFromLocationName(((EditText) v).getText().toString(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (Address a : addrs) {
                    mNote.setText(a.toString() + "\n");
                }
                return true;
            }
        });

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
    protected void onResume() {
        super.onResume();

        populateFields();
    }

    private void populateFields() {
        // TODO Check if there is any unsaved data, if so, load it from the
        // temporary table, otherwise wipe the slate clean.
        ReminderEntry entry = null;
        if (mId >= 0)
            entry = mEntries.getEntry(mId);

        if (entry != null) {
            mLocation.setText(entry.location);
            mNote.setText(entry.note);
        }
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

    private void saveEntry() {
        ReminderEntry entry = null;
        String location = mLocation.getText().toString();
        String note = mNote.getText().toString();
        if (mId >= 0) {
            entry = mEntries.getEntry(mId);

            if (entry != null) {
                entry.location = location;
                entry.note = note;
                mEntries.updateEntry(entry);
            }
        } else {
            entry = new ReminderEntry(location, note);
            mEntries.createEntry(entry);
        }
    }
}
