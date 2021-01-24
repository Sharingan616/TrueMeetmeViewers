package browser;

import java.util.Map;

final class PropertyEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private V value;

    public PropertyEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V oldVal = this.value;
        this.value = value;
        return oldVal;
    }
}
