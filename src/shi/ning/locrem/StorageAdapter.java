package shi.ning.locrem;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class StorageAdapter {
    private static final String TAG = "Locrem";
    private static final String DATABASE_NAME = "locrem";

    private DatabaseHelper mDbHelper;
    protected SQLiteDatabase mDb;
    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
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
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                       + newVersion + ", which destroys all old data");
            db.execSQL("DROP TABLE IF EXISTS " + mTable);
            onCreate(db);
        }
    }

    public StorageAdapter(Context context) {
        mCtx = context;
    }

    protected StorageAdapter open(int version, String table, String create) throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx, version, table, create);
        mDb = mDbHelper.getWritableDatabase();
        mDb.setLockingEnabled(false);
        return this;
    }

    public void close() {
        mDbHelper.close();
    }
}
