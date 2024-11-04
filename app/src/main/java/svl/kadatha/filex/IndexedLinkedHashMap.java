package svl.kadatha.filex;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IndexedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    final ArrayList<K> al_Index = new ArrayList<>();

    @Override
    public V put(K key, V val) {
        if (!super.containsKey(key)) {
            al_Index.add(key);
        }
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


    @Override
    public V remove(Object key) {
        al_Index.remove(key);
        return super.remove(key);
    }


    public boolean removeIndex(K key) {
        return al_Index.remove(key);
    }


    @Override
    public void clear() {
        al_Index.clear();
        super.clear();
    }
}
