package svl.kadatha.filex;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IndexedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    final ArrayList<K> al_Index = new ArrayList<>();

    @Override
    public V put(K key, V val) {
        if (!super.containsKey(key)) al_Index.add(key);
        return super.put(key, val);
    }

    public V getValueAtIndex(int i) {
        return super.get(al_Index.get(i));
    }

    public K getKeyAtIndex(int i) {
        return al_Index.get(i);
    }

    public int getIndexOf(K key) {
        return al_Index.indexOf(key);
    }

    /*
        @Override
        public boolean replace(K key, V oldValue, V newValue)
        {
            // TODO: Implement this method
            return super.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value)
        {
            // TODO: Implement this method
            return super.replace(key, value);
        }
    */
    @Override
    public V remove(Object key) {
        return removeKey((K) key);
    }


    public V removeKey(K key) {
        if (al_Index.remove(key)) {
            return super.remove(key);
        }

        return null;
    }

    public boolean removeIndex(K key) {
        return al_Index.remove(key);
    }


    @Override
    public void clear() {
        // TODO: Implement this method
        al_Index.clear();
        super.clear();
    }


}
