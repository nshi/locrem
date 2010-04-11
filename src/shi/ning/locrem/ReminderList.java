package shi.ning.locrem;

import shi.ning.locrem.ReminderEntry.Columns;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
    static final String TAG = "ReminderList";

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private static final int DELETE_ID = Menu.FIRST + 1;

    private String mQueryString;
    LayoutInflater mLayoutFactory;
    ProximityManagerService mPMService = null;
    private final ServiceConnection mPMConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPMService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPMService = ProximityManagerService.Stub.asInterface(service);
        }
    };

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

            final ImageView indicator =
                (ImageView) view.findViewById(R.id.toggle);
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

            TextView tag = (TextView) view.findViewById(R.id.list_tag);
            if (entry.tag != null) {
                tag.setText(entry.tag);
                tag.setVisibility(View.VISIBLE);
            } else {
                tag.setVisibility(View.GONE);
            }
        }
    }

    void toggleIndicator(ImageView indicator, ReminderEntry entry) {
        final boolean enabled = entry.enabled;
        indicator.setImageResource(enabled ? android.R.drawable.button_onoff_indicator_off
                                          : android.R.drawable.button_onoff_indicator_on);
        entry.enabled = !enabled;

        final Uri uri = ContentUris.withAppendedId(ReminderProvider.CONTENT_URI,
                                                   entry.id);
        if (getContentResolver().update(uri,
                                        ReminderProvider.packEntryToValues(entry),
                                        null, null) == 1) {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, entry.id + " is "
                      + (entry.enabled ? "enabled" : "disabled"));

            try {
                mPMService.onEntryChanged(entry.id);
            } catch (RemoteException e) {}
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "failed to "
                      + (entry.enabled ? "enable" : "disable") + entry.id);
        }
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

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "created");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            startActivity(new Intent(this, Settings.class));
            return true;
        case R.id.menu_show_all:
            setIntent(getIntent().setAction(Intent.ACTION_MAIN));
            mQueryString = null;
            fillData();
            return true;
        }

        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info =
                (AdapterContextMenuInfo) item.getMenuInfo();
            deleteEntry(info.id);
            fillData();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "starting proximity manager service");

        final Intent intent = new Intent(this, ProximityManager.class);
        startService(intent);
        bindService(intent, mPMConnection, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            editEntry(Long.parseLong(intent.getDataString()), false);
            finish();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQueryString = intent.getStringExtra(SearchManager.QUERY);
        } else {
            mQueryString = null;
        }

        fillData();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mPMConnection);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        editEntry(id, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case ACTIVITY_CREATE:
        case ACTIVITY_EDIT:
            if (resultCode == RESULT_OK) {
                final long id = data.getLongExtra(Columns._ID, -1);
                if (id == -1)
                    break;

                try {
                    mPMService.onEntryChanged(id);
                } catch (RemoteException e) {}

                fillData();
            }
            break;
        }
    }

    private void fillData() {
        Cursor c = null;
        if (mQueryString == null)
            c = managedQuery(ReminderProvider.CONTENT_URI,
                             null, null, null, null);
        else
            c = managedQuery(ReminderProvider.CONTENT_SEARCH_URI,
                             null, mQueryString, null, null);

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, c.getCount() + " entries retrieved");
        EntryCursorAdapter notes = new EntryCursorAdapter(this, c);
        setListAdapter(notes);
    }

    void createEntry() {
        startActivityForResult(new Intent(this, ReminderEdit.class),
                               ACTIVITY_CREATE);
    }

    private void editEntry(long id, boolean waitForResult) {
        Intent intent = new Intent(this, ReminderEdit.class);
        intent.putExtra(Columns._ID, id);
        if (waitForResult)
            startActivityForResult(intent, ACTIVITY_EDIT);
        else
            startActivity(intent);
    }

    private void deleteEntry(long id) {
        final Uri uri =
            ContentUris.withAppendedId(ReminderProvider.CONTENT_URI, id);
        if (getContentResolver().delete(uri, null, null) != 1)
            notify("Failed to delete entry " + id, Toast.LENGTH_LONG);
        else
            notify("Successfully deleted entry " + id, Toast.LENGTH_SHORT);
    }

    private void notify(String message, int duration) {
        Context context = getApplicationContext();
        Toast.makeText(context, message, duration).show();
    }
}