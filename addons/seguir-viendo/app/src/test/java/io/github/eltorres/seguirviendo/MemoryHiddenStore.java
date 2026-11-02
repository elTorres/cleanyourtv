package io.github.eltorres.seguirviendo;

import java.util.LinkedHashSet;
import java.util.Set;

final class MemoryHiddenStore implements HiddenStore {
    final Set<String> keys = new LinkedHashSet<>();

    @Override
    public Set<String> keys() {
        return new LinkedHashSet<>(keys);
    }

    @Override
    public void add(String key) {
        keys.add(key);
    }

    @Override
    public void clear() {
        keys.clear();
    }
}
