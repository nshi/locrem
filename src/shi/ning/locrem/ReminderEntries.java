package shi.ning.locrem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.format.Time;

public final class ReminderEntries extends StorageAdapter {
    public static final String KEY_LOCATION = "loc";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_LASTCHECK = "last";
    public static final String KEY_TIME = "time";
    public static final String KEY_ID = "_id";
    public static final String KEY_ENABLED = "enabled";

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
        initialValues.put(KEY_LOCATION, entry.getLocation());
        initialValues.put(KEY_CONTENT, entry.getContent());
        initialValues.put(KEY_ENABLED, entry.isEnabled() ? 1 : 0);
        if (entry.getTime() != null)
            initialValues.put(KEY_TIME, entry.getTime().toMillis(false));
        if (entry.getLastCheck() != null)
            initialValues.put(KEY_LASTCHECK, entry.getLastCheck().toMillis(false));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public int deleteEntry(long id) {
        return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + id, null);
    }

    public Cursor getAllEntries() {
        return mDb.query(DATABASE_TABLE,
                         new String[] { KEY_ID,
                                       KEY_LOCATION,
                                       KEY_CONTENT,
                                       KEY_TIME,
                                       KEY_LASTCHECK,
                                       KEY_ENABLED },
                         null, null, null, null, null);
    }

    public ReminderEntry getEntry(long id) throws SQLException {
        Cursor cursor = mDb.query(true,
                                  DATABASE_TABLE,
                                  new String[] { KEY_LOCATION,
                                                KEY_CONTENT,
                                                KEY_TIME,
                                                KEY_LASTCHECK,
                                                KEY_ENABLED },
                                  KEY_ID + "=" + id,
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
        args.put(KEY_LOCATION, entry.getLocation());
        args.put(KEY_CONTENT, entry.getContent());
        args.put(KEY_ENABLED, entry.isEnabled() ? 1 : 0);
        // XXX not sure if this is gonna work if I don't have all columns
        // present in the args
        if (entry.getTime() != null)
            args.put(KEY_TIME, entry.getTime().toMillis(false));
        if (entry.getLastCheck() != null)
            args.put(KEY_LASTCHECK, entry.getLastCheck().toMillis(false));

        return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + entry.getId(), null) > 0;
    }
}
