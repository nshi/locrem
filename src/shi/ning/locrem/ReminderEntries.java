package shi.ning.locrem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.format.Time;

public final class ReminderEntries extends StorageAdapter {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_TABLE = "entries";
    private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE
                                                  + " (_id INTEGER PRIMARY KEY,"
                                                  + " time INTEGER,"
                                                  + " last INTEGER,"
                                                  + " enabled TINYINT,"
                                                  + " loc text NOT NULL,"
                                                  + " content text NOT NULL);";

    public ReminderEntries(Context context) {
        super(context);
    }

    public ReminderEntries open() {
        return (ReminderEntries) open(DATABASE_VERSION, DATABASE_TABLE, DATABASE_CREATE);
    }

    public long createEntry(ReminderEntry entry) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(ReminderEntry.Columns.LOCATION, entry.getLocation());
        initialValues.put(ReminderEntry.Columns.CONTENT, entry.getContent());
        initialValues.put(ReminderEntry.Columns.ENABLED, entry.isEnabled() ? 1 : 0);
        if (entry.getTime() != null)
            initialValues.put(ReminderEntry.Columns.TIME, entry.getTime().toMillis(false));
        if (entry.getLastCheck() != null)
            initialValues.put(ReminderEntry.Columns.LASTCHECK, entry.getLastCheck()
                                                                    .toMillis(false));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public int deleteEntry(long id) {
        return mDb.delete(DATABASE_TABLE, ReminderEntry.Columns._ID + "=" + id, null);
    }

    public Cursor getAllEntries() {
        return mDb.query(DATABASE_TABLE, ReminderEntry.Columns.QUERY_COLUMNS,
                         null, null, null, null, null);
    }

    public ReminderEntry getEntry(long id) throws SQLException {
        Cursor cursor = mDb.query(true,
                                  DATABASE_TABLE, ReminderEntry.Columns.QUERY_COLUMNS,
                                  ReminderEntry.Columns._ID + "=" + id,
                                  null, null, null, null, null);
        if (cursor == null)
            return null;

        cursor.moveToFirst();

        Time time = null;
        if (!cursor.isNull(2)) {
            time = new Time();
            time.set(cursor.getLong(2));
        }
        Time lastCheck = null;
        if (!cursor.isNull(3)) {
            lastCheck = new Time();
            lastCheck.set(cursor.getLong(3));
        }
        ReminderEntry entry = new ReminderEntry(cursor.getString(0),
                                                cursor.getString(1),
                                                time);
        entry.setLastCheck(lastCheck);

        cursor.close();

        return entry;
    }

    public boolean updateEntry(ReminderEntry entry) {
        ContentValues args = new ContentValues();
        args.put(ReminderEntry.Columns.LOCATION, entry.getLocation());
        args.put(ReminderEntry.Columns.CONTENT, entry.getContent());
        args.put(ReminderEntry.Columns.ENABLED, entry.isEnabled() ? 1 : 0);
        // XXX not sure if this is gonna work if I don't have all columns
        // present in the args
        if (entry.getTime() != null)
            args.put(ReminderEntry.Columns.TIME, entry.getTime().toMillis(false));
        if (entry.getLastCheck() != null)
            args.put(ReminderEntry.Columns.LASTCHECK, entry.getLastCheck()
                                                           .toMillis(false));

        return mDb.update(DATABASE_TABLE, args,
                          ReminderEntry.Columns._ID + "=" + entry.getId(), null) > 0;
    }
}
