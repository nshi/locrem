package shi.ning.locrem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.format.Time;

public final class ReminderEntries extends StorageAdapter {
    public static final String KEY_LOCATION = "loc";
    public static final String KEY_LASTCHECK = "last";
    public static final String KEY_TIME = "time";
    public static final String KEY_ID = "_id";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_TABLE = "entries";
    private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE
                                                  + " (_id INTEGER PRIMARY KEY,"
                                                  + " time INTEGER, last INTEGER,"
                                                  + " loc text NOT NULL);";

    public ReminderEntries(Context context) {
        super(context);
    }

    public ReminderEntries open() {
        return (ReminderEntries) open(DATABASE_VERSION, DATABASE_TABLE, DATABASE_CREATE);
    }

    public long createEntry(ReminderEntry entry) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LOCATION, entry.getLocation());
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
                         new String[] { KEY_ID, KEY_LOCATION, KEY_TIME, KEY_LASTCHECK },
                         null, null, null, null, null);
    }

    public ReminderEntry getEntry(long id) throws SQLException {
        Cursor cursor = mDb.query(true,
                                  DATABASE_TABLE,
                                  new String[] { KEY_LOCATION, KEY_TIME, KEY_LASTCHECK },
                                  KEY_ID + "=" + id,
                                  null, null, null, null, null);
        if (cursor == null)
            return null;

        cursor.moveToFirst();

        Time time = null;
        if (!cursor.isNull(1)) {
            time = new Time();
            time.set(cursor.getLong(1));
        }
        Time lastCheck = null;
        if (!cursor.isNull(2)) {
            lastCheck = new Time();
            lastCheck.set(cursor.getLong(2));
        }
        ReminderEntry entry = new ReminderEntry(time, cursor.getString(0));
        entry.setLastCheck(lastCheck);

        return entry;
    }

    public boolean updateEntry(ReminderEntry entry) {
        ContentValues args = new ContentValues();
        args.put(KEY_LOCATION, entry.getLocation());
        if (entry.getTime() != null)
            args.put(KEY_TIME, entry.getTime().toMillis(false));
        if (entry.getLastCheck() != null)
            args.put(KEY_LASTCHECK, entry.getLastCheck().toMillis(false));

        return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + entry.getId(), null) > 0;
    }
}
