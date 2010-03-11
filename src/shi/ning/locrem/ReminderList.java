package shi.ning.locrem;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public final class ReminderList extends ListActivity {
    public static final String TAG = "locrem";

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private static final int DELETE_ID = Menu.FIRST + 1;

    private ReminderEntries mEntries;
    private ReminderCaches mCaches;
    private LayoutInflater mLayoutFactory;

    private class EntryCursorAdapter extends CursorAdapter {
        public EntryCursorAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mLayoutFactory.inflate(R.layout.row, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ReminderEntry entry = new ReminderEntry(cursor);

            final ImageView indicator = (ImageView) view.findViewById(R.id.toggle);
            indicator.setImageResource(entry.enabled ? android.R.drawable.button_onoff_indicator_on
                                                    : android.R.drawable.button_onoff_indicator_off);

            indicator.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleIndicator(indicator, entry);
                }
            });

            TextView location = (TextView) view.findViewById(R.id.list_location);
            location.setText(entry.location);

            TextView note = (TextView) view.findViewById(R.id.list_note);
            note.setText(entry.note);
        }
    }

    private void toggleIndicator(ImageView indicator, ReminderEntry entry) {
        final boolean enabled = entry.enabled;
        indicator.setImageResource(enabled ? android.R.drawable.button_onoff_indicator_off
                                          : android.R.drawable.button_onoff_indicator_on);
        entry.enabled = !enabled;
        mEntries.updateEntry(entry);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutFactory = LayoutInflater.from(this);

        setContentView(R.layout.main);
        registerForContextMenu(getListView());

        View addAlarm = findViewById(R.id.add_alarm);
        addAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEntry();
            }
        });

        mEntries = new ReminderEntries(this);
        mEntries.open();
        mCaches = new ReminderCaches(this);
        mCaches.open();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            deleteEntry(info.id);
            // TODO should really just remove the one deleted without refreshing
            // the whole list.
            fillData();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        // TODO Auto-generated method stub
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        fillData();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        editEntry(id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case ACTIVITY_CREATE:
        case ACTIVITY_EDIT:
            break;
        }

        fillData();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    private void fillData() {
        Cursor c = mEntries.getAllEntries();
        startManagingCursor(c);

        EntryCursorAdapter notes = new EntryCursorAdapter(this, c);
        setListAdapter(notes);
    }

    private void createEntry() {
        Intent intent = new Intent(this, ReminderEdit.class);
        startActivityForResult(intent, ACTIVITY_CREATE);
    }

    private void editEntry(long id) {
        Intent intent = new Intent(this, ReminderEdit.class);
        intent.putExtra(ReminderEntry.Columns._ID, id);
        startActivityForResult(intent, ACTIVITY_EDIT);
    }

    private void deleteEntry(long id) {
        if (mEntries.deleteEntry(id) != 1)
            notify("Failed to delete entry: " + id, Toast.LENGTH_SHORT);
    }

    private void notify(String message, int duration) {
        Context context = getApplicationContext();
        Toast.makeText(context, message, duration).show();
    }
}