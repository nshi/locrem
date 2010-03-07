package shi.ning.locrem;

import java.util.List;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public final class ReminderEntry implements Parcelable {
    private long mId;
    private Time mTime;
    private Time mLastCheck;
    private String mLocation;
    private List<Address> mAddresses;

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
        mLocation = in.readString();
        in.readTypedList(mAddresses, null);
    }

    public ReminderEntry(String location) {
        this(-1, null, location);
    }

    public ReminderEntry(Time time, String location) {
        this(-1, time, location);
    }

    private ReminderEntry(long id, Time time, String location) {
        mId = id;
        mTime = time;
        mLastCheck = null;
        mLocation = location;
        mAddresses = null;
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

    public List<Address> getAddresses() {
        return mAddresses;
    }

    public void setAddresses(List<Address> addresses) {
        mAddresses = addresses;
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        if (mTime != null)
            dest.writeLong(mTime.toMillis(false));
        else
            dest.writeLong(0);
        if (mLastCheck != null)
            dest.writeLong(mLastCheck.toMillis(false));
        else
            dest.writeLong(0);
        dest.writeString(mLocation);
        dest.writeTypedList(mAddresses);
    }
}
