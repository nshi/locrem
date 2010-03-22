package shi.ning.locrem;

import java.util.LinkedList;

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
                                                  + " note text NOT NULL,"
                                                  + " addrs BLOB NOT NULL);";

    private static final int TRUE = 1;
    private static final int FALSE = 0;

    public ReminderEntries(Context context) {
        super(context);
    }

    public ReminderEntries open() {
        return (ReminderEntries) open(DATABASE_VERSION, DATABASE_TABLE, DATABASE_CREATE);
    }

    public long createEntry(ReminderEntry entry) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(ReminderEntry.Columns.LOCATION, entry.location);
        initialValues.put(ReminderEntry.Columns.NOTE, entry.note);
        initialValues.put(ReminderEntry.Columns.ENABLED, entry.enabled ? TRUE : FALSE);
        initialValues.put(ReminderEntry.Columns.ADDRESSES,
                          ReminderEntry.serializeAddresses(entry.addresses));
        if (entry.time != null)
            initialValues.put(ReminderEntry.Columns.TIME, entry.time.toMillis(false));
        if (entry.lastCheck != null)
            initialValues.put(ReminderEntry.Columns.LASTCHECK,
                              entry.lastCheck.toMillis(false));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public int deleteEntry(long id) {
        return mDb.delete(DATABASE_TABLE, ReminderEntry.Columns._ID + "=" + id, null);
    }

    public Cursor getAllAsCursor() {
        return mDb.query(DATABASE_TABLE, ReminderEntry.Columns.QUERY_COLUMNS,
                         null, null, null, null, null);
    }

    public LinkedList<ReminderEntry> getAllAsEntry() {
        return cursorToEntries(getAllAsCursor());
    }

    public Cursor getEnabledAsCursor() {
        return mDb.query(true,
                         DATABASE_TABLE,
                         ReminderEntry.Columns.QUERY_COLUMNS,
                         ReminderEntry.Columns.ENABLED + "=" + TRUE,
                         null, null, null, null, null);
    }

    public LinkedList<ReminderEntry> getEnabledAsEntry() {
        return cursorToEntries(getEnabledAsCursor());
    }

    public Cursor getDisabledAsCursor() {
        return mDb.query(true,
                         DATABASE_TABLE,
                         ReminderEntry.Columns.QUERY_COLUMNS,
                         ReminderEntry.Columns.ENABLED + "=" + FALSE,
                         null, null, null, null, null);
    }

    public LinkedList<ReminderEntry> getDisabledAsEntry() {
        return cursorToEntries(getDisabledAsCursor());
    }

    public ReminderEntry getEntry(long id) throws SQLException {
        final Cursor cursor = mDb.query(true,
                                        DATABASE_TABLE,
                                        ReminderEntry.Columns.QUERY_COLUMNS,
                                        ReminderEntry.Columns._ID + "=" + id,
                                        null, null, null, null, null);
        if (!cursor.moveToFirst())
            return null;

        final ReminderEntry entry = cursorToEntry(cursor);
        cursor.close();

        return entry;
    }

    public int updateEntry(ReminderEntry entry) {
        ContentValues args = new ContentValues();
        args.put(ReminderEntry.Columns.LOCATION, entry.location);
        args.put(ReminderEntry.Columns.NOTE, entry.note);
        args.put(ReminderEntry.Columns.ENABLED, entry.enabled ? TRUE : FALSE);
        args.put(ReminderEntry.Columns.ADDRESSES,
                 ReminderEntry.serializeAddresses(entry.addresses));
        if (entry.time != null)
            args.put(ReminderEntry.Columns.TIME, entry.time.toMillis(false));
        if (entry.lastCheck != null)
            args.put(ReminderEntry.Columns.LASTCHECK, entry.lastCheck.toMillis(false));

        return mDb.update(DATABASE_TABLE, args,
                          ReminderEntry.Columns._ID + "=" + entry.id, null);
    }

    private LinkedList<ReminderEntry> cursorToEntries(Cursor cursor) {
        if (!cursor.moveToFirst())
            return null;

        final LinkedList<ReminderEntry> entries = new LinkedList<ReminderEntry>();
        do {
            entries.add(cursorToEntry(cursor));
        } while (cursor.moveToNext());

        return entries;
    }

    private ReminderEntry cursorToEntry(Cursor cursor) {
        Time time = null;
        if (!cursor.isNull(ReminderEntry.Columns.TIME_INDEX)) {
            time = new Time();
            time.set(cursor.getLong(ReminderEntry.Columns.TIME_INDEX));
        }
        Time lastCheck = null;
        if (!cursor.isNull(ReminderEntry.Columns.LASTCHECK_INDEX)) {
            lastCheck = new Time();
            lastCheck.set(cursor.getLong(ReminderEntry.Columns.LASTCHECK_INDEX));
        }
        final ReminderEntry entry =
            new ReminderEntry(cursor.getLong(ReminderEntry.Columns.ID_INDEX),
                              cursor.getString(ReminderEntry.Columns.LOCATION_INDEX),
                              cursor.getString(ReminderEntry.Columns.NOTE_INDEX),
                              time, lastCheck,
                              ReminderEntry.deserializeAddresses(cursor.getBlob(ReminderEntry.Columns.ADDRESSES_INDEX)));
        entry.lastCheck = lastCheck;
        entry.enabled =
            cursor.getInt(ReminderEntry.Columns.ENABLED_INDEX) == TRUE ? true : false;

        return entry;
    }
}
