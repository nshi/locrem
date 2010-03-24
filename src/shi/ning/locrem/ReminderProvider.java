package shi.ning.locrem;

import java.util.LinkedList;

import shi.ning.locrem.ReminderEntry.Columns;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

public final class ReminderProvider extends ContentProvider {
    private static final String TAG = "ReminderProvider";
    private static final String AUTHORITY = "shi.ning.locrem.reminderprovider";
    private static final int ENTRIES = 1;
    private static final int ENTRIES_ENABLED = 2;
    private static final int ENTRIES_DISABLED = 3;
    private static final int ENTRIES_ID = 4;
    private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI(AUTHORITY, ReminderEntries.DATABASE_TABLE, ENTRIES);
        mUriMatcher.addURI(AUTHORITY, ReminderEntries.DATABASE_TABLE + "/enabled",
                           ENTRIES_ENABLED);
        mUriMatcher.addURI(AUTHORITY, ReminderEntries.DATABASE_TABLE + "/disabled",
                           ENTRIES_DISABLED);
        mUriMatcher.addURI(AUTHORITY, ReminderEntries.DATABASE_TABLE + "/#",
                           ENTRIES_ID);
    }

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY + "/" + ReminderEntries.DATABASE_TABLE);
    public static final Uri ENABLED_URI =
        Uri.parse("content://" + AUTHORITY + "/" + ReminderEntries.DATABASE_TABLE
                  + "/enabled");
    public static final Uri DISABLED_URI =
        Uri.parse("content://" + AUTHORITY + "/" + ReminderEntries.DATABASE_TABLE
                  + "/disabled");

    private ReminderEntries mEntries;

    public final class ReminderEntries extends StorageAdapter {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_TABLE = "entries";
        private static final String DATABASE_CREATE =
            "CREATE TABLE " + DATABASE_TABLE
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
            super(context, DATABASE_VERSION, DATABASE_TABLE, DATABASE_CREATE);
        }

        @Override
        public long insert(ContentValues values) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.insert(DATABASE_TABLE, null, values);
        }

        @Override
        public int update(long id, ContentValues values) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.update(DATABASE_TABLE, values, Columns._ID + "=" + id, null);
        }

        @Override
        public int delete(long id) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.delete(DATABASE_TABLE, Columns._ID + "=" + id, null);
        }

        public Cursor getAll() {
            return query(null);
        }

        public Cursor getEnabled() {
            return query(Columns.ENABLED + "=" + TRUE);
        }

        public Cursor getDisabled() {
            return query(Columns.ENABLED + "=" + FALSE);
        }

        public Cursor getEntry(long id) throws SQLException {
            return query(Columns._ID + "=" + id);
        }

        private Cursor query(String selection) {
            final SQLiteDatabase db = mDbHelper.getReadableDatabase();
            return db.query(true,
                            DATABASE_TABLE,
                            Columns.QUERY_COLUMNS,
                            selection,
                            null, null, null, null, null);
        }
    }

    public static LinkedList<ReminderEntry> cursorToEntries(Cursor cursor) {
        if (!cursor.moveToFirst())
            return null;

        final LinkedList<ReminderEntry> entries = new LinkedList<ReminderEntry>();
        do {
            entries.add(cursorToEntry(cursor));
        } while (cursor.moveToNext());

        return entries;
    }

    public static ReminderEntry cursorToEntry(Cursor cursor) {
        Time time = null;
        if (!cursor.isNull(Columns.TIME_INDEX)) {
            time = new Time();
            time.set(cursor.getLong(Columns.TIME_INDEX));
        }
        Time lastCheck = null;
        if (!cursor.isNull(Columns.LASTCHECK_INDEX)) {
            lastCheck = new Time();
            lastCheck.set(cursor.getLong(Columns.LASTCHECK_INDEX));
        }
        final byte[] blob = cursor.getBlob(Columns.ADDRESSES_INDEX);
        final ReminderEntry entry =
            new ReminderEntry(cursor.getLong(Columns.ID_INDEX),
                              cursor.getString(Columns.LOCATION_INDEX),
                              cursor.getString(Columns.NOTE_INDEX),
                              time, lastCheck,
                              ReminderEntry.deserializeAddresses(blob));
        entry.lastCheck = lastCheck;
        entry.enabled =
            cursor.getInt(Columns.ENABLED_INDEX) == ReminderEntries.TRUE
            ? true : false;

        return entry;
    }

    public static ContentValues packEntryToValues(ReminderEntry entry) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Columns.LOCATION, entry.location);
        initialValues.put(Columns.NOTE, entry.note);
        initialValues.put(Columns.ENABLED,
                          entry.enabled ? ReminderEntries.TRUE
                                        : ReminderEntries.FALSE);
        initialValues.put(Columns.ADDRESSES,
                          ReminderEntry.serializeAddresses(entry.addresses));
        if (entry.time != null)
            initialValues.put(Columns.TIME,
                              entry.time.toMillis(false));
        if (entry.lastCheck != null)
            initialValues.put(Columns.LASTCHECK, entry.lastCheck.toMillis(false));

        return initialValues;
    }

    @Override
    public String getType(Uri uri) {
        final int match = mUriMatcher.match(uri);
        switch (match) {
        case ENTRIES:
        case ENTRIES_ENABLED:
        case ENTRIES_DISABLED:
            return "vnd.android.cursor.dir/vnd.shi.ning.locrem.entries";
        case ENTRIES_ID:
            return "vnd.android.cursor.item/vnd.shi.ning.locrem.entries";
        default:
            throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public boolean onCreate() {
        mEntries = new ReminderEntries(getContext());
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (mUriMatcher.match(uri) != ENTRIES)
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);

        final long id = mEntries.insert(values);
        if (id < 0)
            throw new SQLException("Failed to insert into URI: " + uri);

        Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "inserted into URI: " + newUri);
        getContext().getContentResolver().notifyChange(newUri, null);

        return newUri;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {
        final int match = mUriMatcher.match(uri);
        Cursor res = null;

        switch (match) {
        case ENTRIES:
            res = mEntries.getAll();
            break;
        case ENTRIES_ENABLED:
            res = mEntries.getEnabled();
            break;
        case ENTRIES_DISABLED:
            res = mEntries.getDisabled();
            break;
        case ENTRIES_ID:
            res = mEntries.getEntry(Long.parseLong(uri.getPathSegments().get(1)));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI");
        }

        if (res == null) {
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "query failed: " + uri);
        } else {
            res.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return res;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {
        if (mUriMatcher.match(uri) != ENTRIES_ID)
            throw new IllegalArgumentException("Cannot delete URI: " + uri);

        final long id = Long.parseLong(uri.getPathSegments().get(1));
        final int count = mEntries.update(id, values);

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "updated " + id);
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (mUriMatcher.match(uri) != ENTRIES_ID)
            throw new IllegalArgumentException("Cannot delete URI: " + uri);

        final int count =
            mEntries.delete(Long.parseLong(uri.getPathSegments().get(1)));

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "deleted " + uri);
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}
