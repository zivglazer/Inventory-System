package dataaccess.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small, bounded write-through cache used by each DAO. It de-dups recently used rows
 * (so the same id isn't loaded twice in a row) and trims DB round-trips, but it NEVER
 * preloads a whole table: once {@code capacity} entries are exceeded the least-recently
 * used one is evicted. The DB file remains the single source of truth.
 */
public class BoundedCache<K, V> {

    private final LinkedHashMap<K, V> map;

    public BoundedCache(final int capacity) {
        // access-order LinkedHashMap → iteration/eviction follows least-recently-used
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized V get(K key)            { return map.get(key); }
    public synchronized void put(K key, V value){ map.put(key, value); }
    public synchronized void remove(K key)      { map.remove(key); }
    public synchronized void clear()            { map.clear(); }
    public synchronized boolean containsKey(K k){ return map.containsKey(k); }
}
