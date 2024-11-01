package svl.kadatha.filex;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A Parcelable implementation of a LinkedHashMap with String keys and String values.
 */
public final class ParcelableStringStringLinkedMap implements Parcelable, Iterable<Map.Entry<String, String>> {
    /**
     * Parcelable.Creator that generates instances of ParcelableStringStringLinkedMap from a Parcel.
     */
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
    private static final int MAX_SIZE = 100000; // Maximum allowable size to prevent memory issues
    private final LinkedHashMap<String, String> map;
    // Tracks structural modifications for fail-fast iterators
    private final List<String> keyList; // Maintains insertion order for efficient access by index

    /**
     * Constructs an empty ParcelableStringStringLinkedMap.
     */
    public ParcelableStringStringLinkedMap() {
        map = new LinkedHashMap<>();
        keyList = new ArrayList<>();
    }

    /**
     * Constructs a ParcelableStringStringLinkedMap from a Parcel.
     *
     * @param in The Parcel to read the map's data from.
     */
    private ParcelableStringStringLinkedMap(Parcel in) {
        int size = in.readInt();
        if (size < 0 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid map size: " + size);
        }
        map = new LinkedHashMap<>(size);
        keyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String key = in.readString();
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
    public void put(String key, String value) {
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
    public String get(String key) {
        return map.get(key);
    }

    /**
     * Adds all entries from the specified map into this map.
     *
     * @param otherMap the map to add from
     */
    public void addAll(Map<String, String> otherMap) {
        for (Map.Entry<String, String> entry : otherMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds all entries from another ParcelableStringStringLinkedMap into this map.
     *
     * @param otherMap the map to add from
     */
    public void addAll(ParcelableStringStringLinkedMap otherMap) {
        for (String key : otherMap.keySet()) {
            put(key, otherMap.get(key));
        }
    }

    /**
     * Returns a set view of the mappings contained in this map.
     *
     * @return a set of map entries
     */
    public Set<Map.Entry<String, String>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }

    /**
     * Returns a set view of the keys contained in this map.
     *
     * @return a set of keys
     */
    public Set<String> keySet() {
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
    public boolean containsKey(String key) {
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
    public String remove(String key) {
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
    public String getKeyAtIndex(int index) {
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
    public LinkedHashMap<String, String> getMap() {
        return new LinkedHashMap<>(map);
    }

    /**
     * Returns an iterator over the entries in the map.
     * This iterator allows safe removal of entries without causing ConcurrentModificationException.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new Iterator<Map.Entry<String, String>>() {
            private final Iterator<String> keyIterator = keyList.iterator();
            private String currentKey = null;

            @Override
            public boolean hasNext() {
                return keyIterator.hasNext();
            }

            @Override
            public Map.Entry<String, String> next() {
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

    /**
     * Serializes the map into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());
        for (String key : keyList) {
            dest.writeString(key);
            dest.writeString(map.get(key));
        }
    }

    /**
     * Describes the kinds of special objects contained in this Parcelable instance's marshaled representation.
     *
     * @return A bitmask indicating the set of special object types marshaled by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Checks if this map is equal to another object.
     *
     * @param o The object to compare with.
     * @return true if the maps are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParcelableStringStringLinkedMap)) {
            return false;
        }
        ParcelableStringStringLinkedMap that = (ParcelableStringStringLinkedMap) o;
        return map.equals(that.map) && keyList.equals(that.keyList);
    }

    /**
     * Returns the hash code value for this map.
     *
     * @return the hash code of the map.
     */
    @Override
    public int hashCode() {
        return Objects.hash(map, keyList);
    }

    /**
     * Returns a string representation of the map.
     *
     * @return A string representing the map's entries.
     */
    @Override
    public String toString() {
        return "ParcelableStringStringLinkedMap{" +
                "map=" + map +
                '}';
    }
}
