package svl.kadatha.filex;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ParcelableStringStringLinkedMap implements Parcelable {
    private LinkedHashMap<String, String> map;

    public ParcelableStringStringLinkedMap() {
        map = new LinkedHashMap<>();
    }

    private ParcelableStringStringLinkedMap(Parcel in) {
        int size = in.readInt();
        map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String value = in.readString();
            map.put(key, value);
        }
    }

    public void put(String key, String value) {
        map.put(key, value);
    }

    public String get(String key) {
        return map.get(key);
    }

    public void addAll(Map<String, String> map) {
        this.map.putAll(map);
    }

    public void addAll(ParcelableStringStringLinkedMap map) {
        this.map.putAll(map.getMap());
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return map.entrySet();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<String> values() {
        return map.values();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public boolean containsValue(String value) {
        return map.containsValue(value);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String remove(String key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public String getKeyAtIndex(int index) {
        if (index < 0 || index >= map.size()) {
            return null;
        }
        int i = 0;
        for (String key : map.keySet()) {
            if (i == index) {
                return key;
            }
            i++;
        }
        return null;
    }


    public LinkedHashMap<String, String> getMap() {
        return map;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParcelableStringStringLinkedMap> CREATOR = new Creator<ParcelableStringStringLinkedMap>() {
        @Override
        public ParcelableStringStringLinkedMap createFromParcel(Parcel in) {
            return new ParcelableStringStringLinkedMap(in);
        }

        @Override
        public ParcelableStringStringLinkedMap[] newArray(int size) {
            return new ParcelableStringStringLinkedMap[size];
        }
    };
}