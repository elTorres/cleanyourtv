package io.github.eltorres.reconectarmandos;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

final class PrefsRemoteStore implements RemoteStore {
    private static final String FILE = "remotes";
    private static final String MARKED = "marked";
    private static final String ATTEMPT_PREFIX = "attempt:";
    private static final char FIELD = '\t';
    private static final char LINE = '\n';

    private final SharedPreferences prefs;

    PrefsRemoteStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public Map<String, String> marked() {
        return decode(prefs.getString(MARKED, ""));
    }

    @Override
    public void mark(Remote remote) {
        Map<String, String> marked = marked();
        marked.put(remote.address(), remote.name());
        prefs.edit().putString(MARKED, encode(marked)).apply();
    }

    @Override
    public void unmark(String address) {
        Map<String, String> marked = marked();
        marked.remove(address);
        prefs.edit().putString(MARKED, encode(marked)).apply();
    }

    @Override
    public Attempt lastAttempt(String address) {
        return Attempt.decode(prefs.getString(ATTEMPT_PREFIX + address, null));
    }

    static String encode(Map<String, String> marked) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : marked.entrySet()) {
            if (out.length() > 0) out.append(LINE);
            out.append(clean(entry.getKey())).append(FIELD).append(clean(entry.getValue()));
        }
        return out.toString();
    }

    static Map<String, String> decode(String stored) {
        Map<String, String> marked = new LinkedHashMap<>();
        if (stored == null || stored.isEmpty()) return marked;
        for (String line : stored.split(String.valueOf(LINE))) {
            int field = line.indexOf(FIELD);
            if (field <= 0) continue;
            marked.put(line.substring(0, field), line.substring(field + 1));
        }
        return marked;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace(FIELD, ' ').replace(LINE, ' ');
    }
}
