package shi.ning.locrem;

import java.util.List;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public final class ReminderEntry implements Parcelable {
    public static final String KEY_ENTRY = "entry";

    private long mId;
    private String mLocation;
    private String mContent;
    private Time mTime;
    private Time mLastCheck;
    private List<Address> mAddresses;
    private boolean mEnabled;

    private ReminderEntry(Parcel in) {
        mId = in.readLong();
        long time = in.readLong();
        mTime = null;
        if (time > 0) {
            mTime = new Time();
            mTime.set(time);
        }
        time = in.readLong();
        mLastCheck = null;
        if (time > 0) {
            mLastCheck = new Time();
            mLastCheck.set(time);
        }
        mEnabled = in.readByte() == 1 ? true : false;
        mLocation = in.readString();
        mContent = in.readString();
        in.readTypedList(mAddresses, null);
    }

    public ReminderEntry(String location, String content) {
        this(-1, location, content, null);
    }

    public ReminderEntry(String location, String content, Time time) {
        this(-1, location, content, time);
    }

    private ReminderEntry(long id, String location, String content, Time time) {
        mId = id;
        mTime = time;
        mLastCheck = null;
        mLocation = location;
        mContent = content;
        mAddresses = null;
        mEnabled = true;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public Time getTime() {
        return mTime;
    }

    public void setTime(Time time) {
        mTime = time;
    }

    public Time getLastCheck() {
        return mLastCheck;
    }

    public void setLastCheck(Time lastCheck) {
        mLastCheck = lastCheck;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public List<Address> getAddresses() {
        return mAddresses;
    }

    public void setAddresses(List<Address> addresses) {
        mAddresses = addresses;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void enabled(boolean enabled) {
        mEnabled = enabled;
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
        out.writeLong(mId);
        if (mTime != null)
            out.writeLong(mTime.toMillis(false));
        else
            out.writeLong(0);
        if (mLastCheck != null)
            out.writeLong(mLastCheck.toMillis(false));
        else
            out.writeLong(0);
        out.writeByte((byte) (mEnabled ? 1 : 0));
        out.writeString(mLocation);
        out.writeString(mContent);
        // XXX not sure if this is gonna work.
        out.writeTypedList(mAddresses);
    }
}
