package io.github.eltorres.reconectarmandos;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class PrefsRemoteStore implements RemoteStore {
    private static final String FILE = "remotes";
    private static final String MARKED = "marked";
    private static final String ATTEMPT_PREFIX = "attempt:";
    private static final String ADVERT_PREFIX = "advert:";
    private static final String BURST_PREFIX = "burst:";
    private static final String UNBONDED_BY_US = "unbonded_by_us";
    private static final String WAIT_UNTIL = "wait_until";
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

    @Override
    public void saveAttempt(String address, Attempt attempt) {
        prefs.edit().putString(ATTEMPT_PREFIX + address, attempt.encode()).apply();
    }

    @Override
    public Set<String> unbondedByUs() {
        return new HashSet<>(prefs.getStringSet(UNBONDED_BY_US, new HashSet<>()));
    }

    @Override
    public void setUnbondedByUs(String address, boolean value) {
        Set<String> unbonded = unbondedByUs();
        if (value) unbonded.add(address); else unbonded.remove(address);
        prefs.edit().putStringSet(UNBONDED_BY_US, unbonded).apply();
    }

    @Override
    public Long lastAdvert(String address) {
        return readTime(ADVERT_PREFIX + address);
    }

    @Override
    public void saveAdvert(String address, long atMs) {
        prefs.edit().putLong(ADVERT_PREFIX + address, atMs).apply();
    }

    @Override
    public Long burstStart(String address) {
        return readTime(BURST_PREFIX + address);
    }

    @Override
    public void setBurstStart(String address, Long atMs) {
        if (atMs == null) prefs.edit().remove(BURST_PREFIX + address).apply();
        else prefs.edit().putLong(BURST_PREFIX + address, atMs).apply();
    }

    @Override
    public long waitUntil() {
        return prefs.getLong(WAIT_UNTIL, 0);
    }

    @Override
    public void setWaitUntil(long atMs) {
        prefs.edit().putLong(WAIT_UNTIL, atMs).apply();
    }

    private Long readTime(String key) {
        return prefs.contains(key) ? prefs.getLong(key, 0) : null;
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
