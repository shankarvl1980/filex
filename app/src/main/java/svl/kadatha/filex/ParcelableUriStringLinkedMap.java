package svl.kadatha.filex;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.*;

/**
 * A Parcelable implementation of a LinkedHashMap with Uri keys and String values.
 */
public class ParcelableUriStringLinkedMap implements Parcelable, Iterable<Map.Entry<Uri, String>> {
    private static final int MAX_SIZE = 100000; // Maximum allowable size to prevent memory issues
    private final LinkedHashMap<Uri, String> map;
    private final List<Uri> keyList; // Maintains insertion order for efficient access by index

    public ParcelableUriStringLinkedMap() {
        map = new LinkedHashMap<>();
        keyList = new ArrayList<>();
    }

    private ParcelableUriStringLinkedMap(Parcel in) {
        int size = in.readInt();
        if (size < 0 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid map size: " + size);
        }
        map = new LinkedHashMap<>(size);
        keyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String keyString = in.readString();
            Uri key = keyString != null ? Uri.parse(keyString) : null;
            String value = in.readString();
            if (key == null) {
                throw new IllegalArgumentException("Null keys are not allowed");
            }
            map.put(key, value);
            keyList.add(key);
        }
    }

    /**
     * Inserts a key-value pair into the map.
     *
     * @param key   the key (must not be null)
     * @param value the value (can be null)
     * @throws IllegalArgumentException if the key is null
     */
    public void put(Uri key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not allowed");
        }
        if (!map.containsKey(key)) {
            keyList.add(key);
        }
        map.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key
     * @return the value, or null if the key doesn't exist
     */
    public String get(Uri key) {
        return map.get(key);
    }

    /**
     * Adds all entries from the specified map into this map.
     *
     * @param otherMap the map to add from
     */
    public void addAll(Map<Uri, String> otherMap) {
        for (Map.Entry<Uri, String> entry : otherMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds all entries from another ParcelableUriStringLinkedMap into this map.
     *
     * @param otherMap the map to add from
     */
    public void addAll(ParcelableUriStringLinkedMap otherMap) {
        for (Uri key : otherMap.keySet()) {
            put(key, otherMap.get(key));
        }
    }

    /**
     * Returns a set view of the mappings contained in this map.
     *
     * @return a set of map entries
     */
    public Set<Map.Entry<Uri, String>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }

    /**
     * Returns a set view of the keys contained in this map.
     *
     * @return a set of keys
     */
    public Set<Uri> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * Returns a collection view of the values contained in this map.
     *
     * @return a collection of values
     */
    public Collection<String> values() {
        return Collections.unmodifiableCollection(map.values());
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        map.clear();
        keyList.clear();
    }

    /**
     * Checks if the map contains the specified key.
     *
     * @param key the key
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(Uri key) {
        return map.containsKey(key);
    }

    /**
     * Checks if the map contains the specified value.
     *
     * @param value the value
     * @return true if the value exists, false otherwise
     */
    public boolean containsValue(String value) {
        return map.containsValue(value);
    }

    /**
     * Checks if the map is empty.
     *
     * @return true if the map is empty, false otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Removes the mapping for the specified key.
     *
     * @param key the key
     * @return the previous value associated with the key, or null if none
     */
    public String remove(Uri key) {
        if (map.containsKey(key)) {
            keyList.remove(key);
            return map.remove(key);
        }
        return null;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the size of the map
     */
    public int size() {
        return map.size();
    }

    /**
     * Retrieves the key at the specified index.
     *
     * @param index the index
     * @return the key at the specified index, or null if index is out of bounds
     */
    public Uri getKeyAtIndex(int index) {
        if (index < 0 || index >= keyList.size()) {
            return null;
        }
        return keyList.get(index);
    }

    /**
     * Returns a copy of the underlying map.
     *
     * @return a LinkedHashMap containing the mappings
     */
    public LinkedHashMap<Uri, String> getMap() {
        return new LinkedHashMap<>(map);
    }


    /**
     * Returns an iterator over the entries in the map.
     * This iterator allows safe removal of entries without causing ConcurrentModificationException.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Map.Entry<Uri, String>> iterator() {
        return new Iterator<Map.Entry<Uri, String>>() {
            private final Iterator<Uri> keyIterator = keyList.iterator();
            private Uri currentKey = null;

            @Override
            public boolean hasNext() {
                return keyIterator.hasNext();
            }

            @Override
            public Map.Entry<Uri, String> next() {
                currentKey = keyIterator.next();
                String value = map.get(currentKey);
                return new AbstractMap.SimpleEntry<>(currentKey, value);
            }

            @Override
            public void remove() {
                if (currentKey == null) {
                    throw new IllegalStateException("next() has not been called or remove() already called");
                }
                keyIterator.remove(); // Removes from keyList
                map.remove(currentKey); // Removes from map
                currentKey = null;
            }
        };
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());
        for (Uri key : keyList) {
            dest.writeString(key.toString());
            dest.writeString(map.get(key));
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
