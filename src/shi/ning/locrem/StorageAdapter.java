package shi.ning.locrem;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class StorageAdapter {
    private static final String TAG = "StorageAdapter";
    private static final String DATABASE_NAME = "locrem";

    protected final DatabaseHelper mDbHelper;

    protected static class DatabaseHelper extends SQLiteOpenHelper {
        private final String mTable;
        private final String mCreate;

        public DatabaseHelper(Context context,
                              int version,
                              String table,
                              String create) {
            super(context, DATABASE_NAME, null, version);

            mTable = table;
            mCreate = create;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(mCreate);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (Log.isLoggable(TAG, Log.WARN))
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                      + newVersion + ", which destroys all old data");
            db.execSQL("DROP TABLE IF EXISTS " + mTable);
            onCreate(db);
        }
    }

    public StorageAdapter(Context context, int version, String table, String create) {
        mDbHelper = new DatabaseHelper(context, version, table, create);
    }

    public abstract long insert(ContentValues values);
    public abstract int update(long id, ContentValues values);
    public abstract int delete(long id);
}
