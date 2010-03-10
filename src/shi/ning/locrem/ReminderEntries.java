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
        initialValues.put(ReminderEntry.Columns.LOCATION, entry.location);
        initialValues.put(ReminderEntry.Columns.CONTENT, entry.content);
        initialValues.put(ReminderEntry.Columns.ENABLED, entry.enabled ? 1 : 0);
        if (entry.time != null)
            initialValues.put(ReminderEntry.Columns.TIME, entry.time.toMillis(false));
        if (entry.lastCheck != null)
            initialValues.put(ReminderEntry.Columns.LASTCHECK, entry.lastCheck.toMillis(false));

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
        entry.lastCheck = lastCheck;

        cursor.close();

        return entry;
    }

    public boolean updateEntry(ReminderEntry entry) {
        ContentValues args = new ContentValues();
        args.put(ReminderEntry.Columns.LOCATION, entry.location);
        args.put(ReminderEntry.Columns.CONTENT, entry.content);
        args.put(ReminderEntry.Columns.ENABLED, entry.enabled ? 1 : 0);
        // XXX not sure if this is gonna work if I don't have all columns
        // present in the args
        if (entry.time != null)
            args.put(ReminderEntry.Columns.TIME, entry.time.toMillis(false));
        if (entry.lastCheck != null)
            args.put(ReminderEntry.Columns.LASTCHECK, entry.lastCheck.toMillis(false));

        return mDb.update(DATABASE_TABLE, args,
                          ReminderEntry.Columns._ID + "=" + entry.id, null) > 0;
    }
}
