package shi.ning.locrem;

import java.util.List;

import android.location.Address;
import android.text.format.Time;

public final class ReminderEntry {
    private long mId;
    private Time mTime;
    private Time mLastCheck;
    private String mLocation;
    private List<Address> mAddresses;

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
}
