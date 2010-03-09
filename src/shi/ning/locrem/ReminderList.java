package shi.ning.locrem;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public final class ReminderList extends ListActivity {
    public static final String TAG = "locrem";

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private static final int ADD_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private ReminderEntries mEntries;
    private ReminderCaches mCaches;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        registerForContextMenu(getListView());

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
            // XXX this is not gonna work, I don't need the rowId, I need the id
            // I set.
            deleteEntry(info.id);
            // TODO should really just remove the one deleted without refreshing
            // the whole list.
            fillData();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_ID, 0, R.string.menu_add);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ADD_ID:
            createEntry();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
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

        String[] from = new String[] { ReminderEntries.KEY_LOCATION,
                                      ReminderEntries.KEY_CONTENT };
        int[] to = new int[] { R.id.list_location, R.id.list_content };

        SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
                                                            R.layout.row,
                                                            c,
                                                            from,
                                                            to);
        setListAdapter(notes);
    }

    private void createEntry() {
        Intent intent = new Intent(this, ReminderEdit.class);
        startActivityForResult(intent, ACTIVITY_CREATE);
    }

    private void editEntry(long id) {
        Intent intent = new Intent(this, ReminderEdit.class);
        intent.putExtra(ReminderEntries.KEY_ID, id);
        startActivityForResult(intent, ACTIVITY_EDIT);
    }

    private void deleteEntry(long id) {
        if (mEntries.deleteEntry(id) != 1) {
            // TODO display a toast warning.
        }
    }
}