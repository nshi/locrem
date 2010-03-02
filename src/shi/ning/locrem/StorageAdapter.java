package shi.ning.locrem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StorageAdapter {
    public static final String KEY_LOCATION = "loc";
    public static final String KEY_TIME = "time";
    public static final String KEY_ID = "_id";

    private static final String TAG = "Locrem";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "locrem";
    private static final String DATABASE_TABLE = "entries";
    private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE
                                                  + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                                  + "loc text NOT NULL, time INTEGER NOT NULL);";

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                       + newVersion + ", which destroys all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    public StorageAdapter(Context context) {
        mCtx = context;
    }

    public StorageAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public long createEntry(String loc, long time) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LOCATION, loc);
        initialValues.put(KEY_TIME, time);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public int deleteEntry(long id) {
        return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + id, null);
    }

    public Cursor getAllEntries() {
        return mDb.query(DATABASE_TABLE,
                         new String[] { KEY_ID, KEY_LOCATION, KEY_TIME },
                         null, null, null, null, null);
    }

    public Cursor getEntry(long id) throws SQLException {
        Cursor cursor = mDb.query(true,
                                  DATABASE_TABLE,
                                  new String[] { KEY_ID, KEY_LOCATION, KEY_TIME },
                                  KEY_ID + "=" + id,
                                  null, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        return cursor;
    }

    public boolean updateEntry(long id, String loc, long time) {
        ContentValues args = new ContentValues();
        args.put(KEY_LOCATION, loc);
        args.put(KEY_TIME, time);

        return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + id, null) > 0;
    }
}
