package shi.ning.locrem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public final class ReminderEdit extends Activity {
    private ReminderEntries mEntries;
    private long mId;

    private EditText mLocation;
    private EditText mContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mLocation = (EditText) findViewById(R.id.location);
        mContent = (EditText) findViewById(R.id.content);

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
            mContent.setText(entry.content);
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
        String content = mContent.getText().toString();
        if (mId >= 0) {
            entry = mEntries.getEntry(mId);

            if (entry != null) {
                entry.location = location;
                entry.content = content;
                mEntries.updateEntry(entry);
            }
        } else {
            entry = new ReminderEntry(location, content);
            mEntries.createEntry(entry);
        }
    }
}
