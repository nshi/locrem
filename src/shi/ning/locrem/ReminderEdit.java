package shi.ning.locrem;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ReminderEdit extends Activity {
    private ReminderEntry mEntry;

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

        if (savedInstanceState != null)
            mEntry = savedInstanceState.getParcelable(ReminderEntry.KEY_ENTRY);
        else
            mEntry = getIntent().getParcelableExtra(ReminderEntry.KEY_ENTRY);

        populateFields();

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO I'm not sure what I want to put here now.
                setResult(RESULT_OK);
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO I'm not sure what I want to put here now.
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    private void populateFields() {
        // TODO should fill in all the stuff.
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }
}
