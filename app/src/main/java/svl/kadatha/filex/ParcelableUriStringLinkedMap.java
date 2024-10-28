package svl.kadatha.filex;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ParcelableUriStringLinkedMap implements Parcelable {
    private LinkedHashMap<Uri, String> map;

    public ParcelableUriStringLinkedMap() {
        map = new LinkedHashMap<>();
    }

    private ParcelableUriStringLinkedMap(Parcel in) {
        int size = in.readInt();
        map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            Uri key = Uri.parse(in.readString());
            String value = in.readString();
            map.put(key, value);
        }
    }

    public void put(Uri key, String value) {
        map.put(key, value);
    }

    public String get(Uri key) {
        return map.get(key);
    }

    public void addAll(Map<Uri, String> map) {
        this.map.putAll(map);
    }

    public void addAll(ParcelableUriStringLinkedMap map) {
        this.map.putAll(map.getMap());
    }

    public Set<Map.Entry<Uri, String>> entrySet() {
        return map.entrySet();
    }

    public Set<Uri> keySet() {
        return map.keySet();
    }

    public Collection<String> values() {
        return map.values();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Uri key) {
        return map.containsKey(key);
    }

    public boolean containsValue(String value) {
        return map.containsValue(value);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String remove(Uri key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public LinkedHashMap<Uri, String> getMap() {
        return map;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());
        for (Map.Entry<Uri, String> entry : map.entrySet()) {
            dest.writeString(entry.getKey().toString());
            dest.writeString(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParcelableUriStringLinkedMap> CREATOR = new Creator<ParcelableUriStringLinkedMap>() {
        @Override
        public ParcelableUriStringLinkedMap createFromParcel(Parcel in) {
            return new ParcelableUriStringLinkedMap(in);
        }

        @Override
        public ParcelableUriStringLinkedMap[] newArray(int size) {
            return new ParcelableUriStringLinkedMap[size];
        }
    };
}