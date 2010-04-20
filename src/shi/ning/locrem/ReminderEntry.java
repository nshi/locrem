package shi.ning.locrem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import android.database.Cursor;
import android.location.Address;
import android.provider.BaseColumns;
import android.text.format.Time;

public final class ReminderEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public long id;
    public String location;
    public String note;
    public Time time;
    public Time lastCheck;
    public List<Address> addresses;
    public String tag;
    public boolean enabled;

    public static final class Columns implements BaseColumns {
        public static final String LOCATION = "loc";
        public static final String ADDRESSES = "addrs";
        public static final String TAG = "tag";
        public static final String NOTE = "note";
        public static final String LASTCHECK = "last";
        public static final String TIME = "time";
        public static final String ENABLED = "enabled";

        public static final String[] QUERY_COLUMNS = {_ID,
                                                      TIME,
                                                      LASTCHECK,
                                                      ENABLED,
                                                      LOCATION,
                                                      NOTE,
                                                      TAG,
                                                      ADDRESSES};

        // Have to be in sync with QUERY_COLUMNS
        public static final int ID_INDEX = 0;
        public static final int TIME_INDEX = 1;
        public static final int LASTCHECK_INDEX = 2;
        public static final int ENABLED_INDEX = 3;
        public static final int LOCATION_INDEX = 4;
        public static final int NOTE_INDEX = 5;
        public static final int TAG_INDEX = 6;
        public static final int ADDRESSES_INDEX = 7;
    }

    public ReminderEntry(Cursor in) {
        this.id = in.getLong(Columns.ID_INDEX);
        long time = in.getLong(Columns.TIME_INDEX);
        this.time = new Time();
        this.time.setToNow();
        this.time.second = 0;
        if (time > 0) {
            this.time.set(time);
        }
        time = in.getLong(Columns.LASTCHECK_INDEX);
        this.lastCheck = null;
        if (time > 0) {
            this.lastCheck = new Time();
            this.lastCheck.set(time);
        }
        this.enabled = in.getInt(Columns.ENABLED_INDEX) == 1;
        this.location = in.getString(Columns.LOCATION_INDEX);
        this.note = in.getString(Columns.NOTE_INDEX);
        this.tag = in.getString(Columns.TAG_INDEX);
        this.addresses =
            deserializeAddresses(in.getBlob(Columns.ADDRESSES_INDEX));
    }

    public ReminderEntry(String location, String note,
                         List<Address> addresses) {
        this(-1, location, note, null, null, null, addresses);
    }

    public ReminderEntry(String location, String note, Time time,
                         List<Address> addresses) {
        this(-1, location, note, time, null, null, addresses);
    }

    public ReminderEntry(long id, String location, String note,
                         Time time, Time lastCheck, String tag,
                         List<Address> addresses) {
        this.id = id;
        if (time == null) {
            time = new Time();
            time.setToNow();
        }
        this.time = time;
        this.lastCheck = null;
        this.location = location;
        this.note = note;
        this.tag = tag;
        this.addresses = addresses;
        this.enabled = true;
    }

    public static byte[] serializeAddresses(List<Address> addresses) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(buffer);

        // Length preceded byte array
        try {
            final int length = addresses.size();
            out.writeByte(length);
            for (int i = 0; i < length; i++) {
                final Address a = addresses.get(i);
                /*
                 * longitude
                 * latitude
                 * admin area
                 * sub admin area
                 * locality
                 * thoroughfare
                 * feature name
                 */
                final String admin = a.getAdminArea();
                final String subAdmin = a.getSubAdminArea();
                final String locality = a.getLocality();
                final String thoroughfare = a.getThoroughfare();
                final String feature = a.getFeatureName();

                out.writeDouble(a.getLongitude());
                out.writeDouble(a.getLatitude());
                out.writeUTF(admin != null ? admin : "");
                out.writeUTF(subAdmin != null ? subAdmin : "");
                out.writeUTF(locality != null ? locality : "");
                out.writeUTF(thoroughfare != null ? thoroughfare : "");
                out.writeUTF(feature != null ? feature : "");
            }
        } catch (IOException e) {
            return null;
        }

        return buffer.toByteArray();
    }

    public static List<Address> deserializeAddresses(byte[] buffer) {
        if (buffer == null)
            return null;

        final LinkedList<Address> addresses = new LinkedList<Address>();
        final ByteArrayInputStream buf = new ByteArrayInputStream(buffer);
        final DataInputStream in = new DataInputStream(buf);

        try {
            final byte size = in.readByte();
            for (int i = 0; i < size; i++) {
                /*
                 * longitude
                 * latitude
                 * admin area
                 * sub admin area
                 * locality
                 * thoroughfare
                 * feature name
                 */
                final Address a = new Address(Locale.getDefault());

                a.setLongitude(in.readDouble());
                a.setLatitude(in.readDouble());
                final String admin = in.readUTF();
                final String subAdmin = in.readUTF();
                final String locality = in.readUTF();
                final String thoroughfare = in.readUTF();
                final String feature = in.readUTF();

                if (admin.length() > 0)
                    a.setAdminArea(admin);
                if (subAdmin.length() > 0)
                    a.setSubAdminArea(subAdmin);
                if (locality.length() > 0)
                    a.setLocality(locality);
                if (thoroughfare.length() > 0)
                    a.setThoroughfare(thoroughfare);
                if (feature.length() > 0)
                    a.setFeatureName(feature);

                addresses.add(a);
            }
        } catch (IOException e) {
            return null;
        }

        return addresses;
    }
}
