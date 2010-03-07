package shi.ning.locrem;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Address;

public final class ReminderCaches extends StorageAdapter {
    public static final String KEY_ADDRESSES = "addrs";
    public static final String KEY_ID = "_id";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_TABLE = "caches";
    private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE
                                                  + " (_id INTEGER PRIMARY KEY,"
                                                  + " addrs text NOT NULL);";

    public ReminderCaches(Context context) {
        super(context);
    }

    public ReminderCaches open() {
        return (ReminderCaches) open(DATABASE_VERSION, DATABASE_TABLE, DATABASE_CREATE);
    }

    public long createCache(long id, List<Address> addresses) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ID, id);
        initialValues.put(KEY_ADDRESSES, addressesToString(addresses));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public int deleteCache(long id) {
        return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + id, null);
    }

    public List<Address> getCache(long id) throws SQLException {
        Cursor cursor = mDb.query(true,
                                  DATABASE_TABLE,
                                  new String[] { KEY_ADDRESSES },
                                  KEY_ID + "=" + id,
                                  null, null, null, null, null);
        if (cursor == null)
            return null;

        cursor.moveToFirst();

        List<Address> addresses = new LinkedList<Address>();
        String addressList = cursor.getString(0);
        int start = addressList.indexOf(';');
        int end = addressList.indexOf(';', start);
        for (; end > start;) {
            String addressPair = addressList.substring(start, end);
            Address address = new Address(Locale.getDefault());
            int commaIndex = addressPair.indexOf(',');
            address.setLatitude(Double.parseDouble(addressPair.substring(0, commaIndex)));
            address.setLongitude(Double.parseDouble(addressPair.substring(commaIndex)));

            start = addressList.indexOf(';', end);
            end = addressList.indexOf(';', start);
        }

        cursor.close();

        return addresses;
    }

    public boolean updateCache(long id, List<Address> addresses) {
        ContentValues args = new ContentValues();
        args.put(KEY_ADDRESSES, addressesToString(addresses));

        return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + id, null) > 0;
    }

    private String addressesToString(List<Address> addresses) {
        String addressList = "";

        for (Address address : addresses)
            addressList += address.getLatitude() + "," + address.getLongitude() + ";";

        return addressList.substring(0, addressList.length());
    }
}
