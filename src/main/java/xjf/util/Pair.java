/*
 * OpenJDK 8 不支持 javafx.util, 折腾了半天决定还是自己重写一个 Pair.
 * 本 Pair 类内容与 javafx.util.Pair 基本一致.
 */

package xjf.util;

import java.util.Objects;

public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof xjf.util.Pair) {
            xjf.util.Pair<?, ?> pair = (xjf.util.Pair<?, ?>) o;
            if (!Objects.equals(key, pair.key)) return false;
            return Objects.equals(value, pair.value);
        }
        return false;
    }
}
