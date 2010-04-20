package shi.ning.locrem;

interface ProximityManagerService {
    void onEntryChanged(long id);
    byte[] getCurrentLocation();
}
