package io.github.eltorres.seguirviendo;

import java.util.Collection;

interface SecretStore {
    void put(String key, String value);

    String get(String key);

    void remove(String key);

    Collection<String> values();

    default String scrub(String text) {
        if (text == null) return null;
        String out = text;
        for (String secret : values()) {
            if (secret != null && secret.length() >= 4) out = out.replace(secret, "***");
        }
        return out.replaceAll("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+", "$1***");
    }
}
