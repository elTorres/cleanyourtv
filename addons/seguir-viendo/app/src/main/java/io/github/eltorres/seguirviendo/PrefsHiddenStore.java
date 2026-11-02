package io.github.eltorres.seguirviendo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PrefsHiddenStore implements HiddenStore {
    static final int MAX_KEYS = 200;
    private static final String KEYS = "keys";
    private static final String LINE = "\n";

    private final SharedPreferences prefs;

    PrefsHiddenStore(Context context) {
        prefs = context.getSharedPreferences("hidden", Context.MODE_PRIVATE);
    }

    @Override
    public synchronized Set<String> keys() {
        return parse(prefs.getString(KEYS, ""));
    }

    @Override
    public synchronized void add(String key) {
        prefs.edit().putString(KEYS, append(prefs.getString(KEYS, ""), key)).commit();
    }

    static String append(String stored, String key) {
        List<String> keys = new ArrayList<>(parse(stored));
        keys.remove(key);
        keys.add(key);
        List<String> kept = keys.subList(Math.max(0, keys.size() - MAX_KEYS), keys.size());
        return String.join(LINE, kept);
    }

    static Set<String> parse(String stored) {
        Set<String> keys = new LinkedHashSet<>();
        if (stored == null) return keys;
        for (String key : stored.split(LINE)) {
            if (!key.isEmpty()) keys.add(key);
        }
        return keys;
    }
}
