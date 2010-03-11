package shi.ning.locrem;

import java.util.List;

import android.database.Cursor;
import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.format.Time;

public final class ReminderEntry implements Parcelable {
    public static final String KEY_ENTRY = "entry";

    public long id;
    public String location;
    public String note;
    public Time time;
    public Time lastCheck;
    public List<Address> addresses;
    public boolean enabled;

    public static final class Columns implements BaseColumns {
        public static final String LOCATION = "loc";
        public static final String NOTE = "note";
        public static final String LASTCHECK = "last";
        public static final String TIME = "time";
        public static final String ENABLED = "enabled";

        public static final String[] QUERY_COLUMNS = { _ID,
                                                      LOCATION,
                                                      NOTE,
                                                      LASTCHECK,
                                                      TIME,
                                                      ENABLED };

        // Have to be in sync with QUERY_COLUMNS
        public static final int ID_INDEX = 0;
        public static final int LOCATION_INDEX = 1;
        public static final int NOTE_INDEX = 2;
        public static final int LASTCHECK_INDEX = 3;
        public static final int TIME_INDEX = 4;
        public static final int ENABLED_INDEX = 5;
    }

    private ReminderEntry(Parcel in) {
        this.id = in.readLong();
        long time = in.readLong();
        this.time = null;
        if (time > 0) {
            this.time = new Time();
            this.time.set(time);
        }
        time = in.readLong();
        this.lastCheck = null;
        if (time > 0) {
            this.lastCheck = new Time();
            this.lastCheck.set(time);
        }
        this.enabled = in.readByte() == 1;
        this.location = in.readString();
        this.note = in.readString();
        in.readTypedList(this.addresses, null);
    }

    public ReminderEntry(Cursor in) {
        this.id = in.getLong(Columns.ID_INDEX);
        long time = in.getLong(Columns.TIME_INDEX);
        this.time = null;
        if (time > 0) {
            this.time = new Time();
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
        this.addresses = null;
    }

    public ReminderEntry(String location, String note) {
        this(-1, location, note, null);
    }

    public ReminderEntry(String location, String note, Time time) {
        this(-1, location, note, time);
    }

    private ReminderEntry(long id, String location, String note, Time time) {
        this.id = id;
        this.time = time;
        this.lastCheck = null;
        this.location = location;
        this.note = note;
        this.addresses = null;
        this.enabled = true;
    }

    public static final Parcelable.Creator<ReminderEntry> CREATE = new Parcelable.Creator<ReminderEntry>() {
        @Override
        public ReminderEntry createFromParcel(Parcel source) {
            return new ReminderEntry(source);
        }

        @Override
        public ReminderEntry[] newArray(int size) {
            return new ReminderEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.id);
        if (this.time != null)
            out.writeLong(this.time.toMillis(false));
        else
            out.writeLong(0);
        if (this.lastCheck != null)
            out.writeLong(this.lastCheck.toMillis(false));
        else
            out.writeLong(0);
        out.writeByte((byte) (this.enabled ? 1 : 0));
        out.writeString(this.location);
        out.writeString(this.note);
        // XXX not sure if this is gonna work.
        out.writeTypedList(this.addresses);
    }
}
