package io.github.eltorres.seguirviendo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

final class MemorySecretStore implements SecretStore {
    final Map<String, String> values = new LinkedHashMap<>();

    @Override
    public void put(String key, String value) {
        values.put(key, value);
    }

    @Override
    public String get(String key) {
        return values.get(key);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }

    @Override
    public Collection<String> values() {
        return values.values();
    }
}
