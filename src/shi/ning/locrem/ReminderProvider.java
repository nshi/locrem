package shi.ning.locrem;

import java.util.LinkedList;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public final class ReminderProvider extends ContentProvider {
    static final String TAG = "ReminderProvider";
    private static final String AUTHORITY = "shi.ning.locrem.reminderprovider";
    private static final int SUGGEST_ENTRIES = 0;
    private static final int ENTRIES = 1;
    private static final int SEARCH_ENTRIES = 2;
    private static final int ENTRIES_ENABLED = 3;
    private static final int ENTRIES_DISABLED = 4;
    private static final int ENTRIES_ID = 5;
    private static final int ENTRIES_TAGS = 6;
    private static final int RECENT = 7;
    private static final UriMatcher mUriMatcher =
        new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                           SUGGEST_ENTRIES);
        mUriMatcher.addURI(AUTHORITY,
                           SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                           SUGGEST_ENTRIES);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE, ENTRIES);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE + "/search",
                           SEARCH_ENTRIES);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE + "/enabled",
                           ENTRIES_ENABLED);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE + "/disabled",
                           ENTRIES_DISABLED);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE + "/#",
                           ENTRIES_ID);
        mUriMatcher.addURI(AUTHORITY, Database.ENTRIES_TABLE + "/tags",
                           ENTRIES_TAGS);
        mUriMatcher.addURI(AUTHORITY, Database.RECENT_TABLE, RECENT);
    }

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.ENTRIES_TABLE);
    public static final Uri CONTENT_SEARCH_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.ENTRIES_TABLE
                  + "/search");
    public static final Uri ENABLED_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.ENTRIES_TABLE
                  + "/enabled");
    public static final Uri DISABLED_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.ENTRIES_TABLE
                  + "/disabled");
    public static final Uri TAGS_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.ENTRIES_TABLE
                  + "/tags");
    public static final Uri RECENT_URI =
        Uri.parse("content://" + AUTHORITY + "/" + Database.RECENT_TABLE);

    private Database mDb;

    private static final String[] SuggestColumns = {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    public static final class RecentColumns implements BaseColumns {
        public static final String ADDRESS = "address";

        public static final String[] QUERY_COLUMNS = {_ID,
                                                      ADDRESS};

        // Have to be in sync with QUERY_COLUMNS
        public static final int ID_INDEX = 0;
        public static final int ADDRESS_INDEX = 1;
    }

    private static final class Database {
        private static final String DATABASE_NAME = "locrem";
        private static final int DATABASE_VERSION = 1;

        private static final String ENTRIES_TABLE = "entries";
        private static final String ENTRIES_CREATE =
            "CREATE TABLE " + ENTRIES_TABLE
            + " (_id INTEGER PRIMARY KEY,"
            + " time INTEGER DEFAULT NULL,"
            + " last INTEGER DEFAULT NULL,"
            + " enabled TINYINT DEFAULT 0,"
            + " loc text NOT NULL,"
            + " tag text DEFAULT NULL,"
            + " note text NOT NULL,"
            + " addrs BLOB NOT NULL);";

        private static final String RECENT_TABLE = "recent";
        private static final int RECENT_TABLE_SIZE = 10;
        private static final String RECENT_CREATE =
            "CREATE TABLE " + RECENT_TABLE
            + " (_id INTEGER PRIMARY KEY,"
            + " address TEXT NOT NULL UNIQUE);";

        private final DatabaseHelper mDbHelper;

        private static class DatabaseHelper extends SQLiteOpenHelper {
            public DatabaseHelper(Context context) {
                super(context, DATABASE_NAME, null, DATABASE_VERSION);

                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "created database helper");
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "creating table " + ENTRIES_TABLE
                          + ": " + ENTRIES_CREATE);
                db.execSQL(ENTRIES_CREATE);

                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "creating table " + RECENT_TABLE
                          + ": " + RECENT_CREATE);
                db.execSQL(RECENT_CREATE);
                //db.execSQL(RECENT_LIMIT);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion,
                                  int newVersion) {
                if (Log.isLoggable(TAG, Log.WARN))
                    Log.w(TAG, "Upgrading database from version " + oldVersion
                          + " to " + newVersion
                          + ", which destroys all old data");
                db.execSQL("DROP TABLE IF EXISTS " + ENTRIES_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + RECENT_TABLE);
                onCreate(db);
            }
        }

        public Database(Context context) {
            mDbHelper = new DatabaseHelper(context);
        }

        public long insert(String table, ContentValues values) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.insert(table, null, values);
        }

        public int update(String table, long id, ContentValues values) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.update(table, values, "_id=" + id, null);
        }

        public int delete(String table, String whereClause,
                          String[] whereArgs) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            return db.delete(table, whereClause, whereArgs);
        }

        private Cursor query(String table, String[] columns, String selection) {
            final SQLiteDatabase db = mDbHelper.getReadableDatabase();
            return db.query(true, table, columns, selection,
                            null, null, null, null, null);
        }
    }

    public static LinkedList<ReminderEntry> cursorToEntries(Cursor cursor) {
        if (!cursor.moveToFirst())
            return null;

        final LinkedList<ReminderEntry> entries =
            new LinkedList<ReminderEntry>();
        do {
            entries.add(new ReminderEntry(cursor));
        } while (cursor.moveToNext());

        return entries;
    }

    @Override
    public String getType(Uri uri) {
        final int match = mUriMatcher.match(uri);
        switch (match) {
        case ENTRIES:
        case ENTRIES_ENABLED:
        case ENTRIES_DISABLED:
        case ENTRIES_TAGS:
            return "vnd.android.cursor.dir/vnd.shi.ning.locrem.entries";
        case ENTRIES_ID:
            return "vnd.android.cursor.item/vnd.shi.ning.locrem.entries";
        case RECENT:
            return "vnd.android.cursor.dir/vnd.shi.ning.locrem.recent";
        default:
            throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public boolean onCreate() {
        mDb = new Database(getContext());
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int type = mUriMatcher.match(uri);
        if (type != ENTRIES && type != RECENT)
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);

        long id;
        if (type == ENTRIES) {
            id = mDb.insert(Database.ENTRIES_TABLE, values);
        } else {
            id = mDb.insert(Database.RECENT_TABLE, values);
            if (id < 0) {
                if (Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG, "address already exists in recent: "
                          + values.getAsString(RecentColumns.ADDRESS));
                return null;
            }

            final int count = mDb.delete(Database.RECENT_TABLE,
                                         BaseColumns._ID + " <= (SELECT MAX("
                                         + BaseColumns._ID + ") FROM "
                                         + Database.RECENT_TABLE + ") - "
                                         + Database.RECENT_TABLE_SIZE, null);
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "deleted " + count + " oldest entries");
        }
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
        case SUGGEST_ENTRIES:
            String query = null;
            if (uri.getPathSegments().size() > 1) {
                query = uri.getLastPathSegment().toLowerCase();
            }
            res = getSuggestions(query);
            break;
        case ENTRIES:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            null);
            break;
        case SEARCH_ENTRIES:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            buildSearchString(selection));
            break;
        case ENTRIES_ENABLED:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            ReminderEntry.Columns.ENABLED + "="
                            + ReminderEntry.TRUE);
            break;
        case ENTRIES_DISABLED:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            ReminderEntry.Columns.ENABLED + "="
                            + ReminderEntry.FALSE);
            break;
        case ENTRIES_ID:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            ReminderEntry.Columns._ID + "="
                            + Long.parseLong(uri.getPathSegments().get(1)));
            break;
        case ENTRIES_TAGS:
            res = mDb.query(Database.ENTRIES_TABLE,
                            ReminderEntry.Columns.QUERY_COLUMNS,
                            selection);
            break;
        case RECENT:
            res = mDb.query(Database.RECENT_TABLE, RecentColumns.QUERY_COLUMNS,
                            selection);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
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
        final int count = mDb.update(Database.ENTRIES_TABLE, id, values);

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "updated " + id);
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = mUriMatcher.match(uri);
        if (match != ENTRIES_ID)
            throw new IllegalArgumentException("Cannot delete URI: " + uri);

        final int count =
            mDb.delete(Database.ENTRIES_TABLE, BaseColumns._ID + " = ?",
                       new String[] {uri.getPathSegments().get(1)});

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "deleted " + uri);
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    private static String buildSearchString(String query) {
        return ReminderEntry.Columns.LOCATION + " LIKE '%"
               + query + "%' OR " + ReminderEntry.Columns.NOTE
               + " LIKE '%" + query + "%' OR "
               + ReminderEntry.Columns.TAG + " LIKE '%"
               + query + "%'";
    }

    private Cursor getSuggestions(String query) {
        final String processedQuery = query == null ? "" : query;
        final String selection = buildSearchString(processedQuery);
        final Cursor c = mDb.query(Database.ENTRIES_TABLE,
                                   ReminderEntry.Columns.QUERY_COLUMNS,
                                   selection);
        final MatrixCursor res = new MatrixCursor(SuggestColumns);

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        do {
            final long id = c.getLong(ReminderEntry.Columns.ID_INDEX);
            final Object[] row =
                new Object[] {id,
                              c.getString(ReminderEntry.Columns.NOTE_INDEX),
                              c.getString(ReminderEntry.Columns.LOCATION_INDEX),
                              id};
            res.addRow(row);
        } while (c.moveToNext());

        c.close();
        return res;
    }
}
