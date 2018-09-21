package com.me94me.resource_lifecycle.lifecycle;

import com.me94me.resource_lifecycle.livedata.SafeIterableMap;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Poor's man LinkedHashMap, which supports modifications during iterations.
 * Takes more memory that {@link SafeIterableMap}
 * It is NOT thread safe.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FastSafeIterableMap<K, V> extends SafeIterableMap<K, V> {

    private HashMap<K, Entry<K, V>> mHashMap = new HashMap<>();

    @Override
    protected Entry<K, V> get(K k) {
        return mHashMap.get(k);
    }

    @Override
    public V putIfAbsent(@NonNull K key, @NonNull V v) {
        Entry<K, V> current = get(key);
        if (current != null) {
            return current.mValue;
        }
        mHashMap.put(key, put(key, v));
        return null;
    }

    @Override
    public V remove(@NonNull K key) {
        V removed = super.remove(key);
        mHashMap.remove(key);
        return removed;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     */
    public boolean contains(K key) {
        return mHashMap.containsKey(key);
    }

    /**
     * Return an entry added to prior to an entry associated with the given key.
     *
     * @param k the key
     */
    public Map.Entry<K, V> ceil(K k) {
        if (contains(k)) {
            return mHashMap.get(k).mPrevious;
        }
        return null;
    }
}
