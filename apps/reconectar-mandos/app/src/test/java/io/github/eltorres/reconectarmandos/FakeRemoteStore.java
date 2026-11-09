package io.github.eltorres.reconectarmandos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class FakeRemoteStore implements RemoteStore {
    private final Map<String, String> marked = new LinkedHashMap<>();
    private final Map<String, Attempt> attempts = new HashMap<>();
    private final Set<String> unbondedByUs = new HashSet<>();
    private final Map<String, Long> adverts = new HashMap<>();
    private final Map<String, Long> bursts = new HashMap<>();
    private long waitUntil;

    FakeRemoteStore with(String address) {
        marked.put(address, address);
        return this;
    }

    @Override
    public Map<String, String> marked() {
        return new LinkedHashMap<>(marked);
    }

    @Override
    public void mark(Remote remote) {
        marked.put(remote.address(), remote.name());
    }

    @Override
    public void unmark(String address) {
        marked.remove(address);
    }

    @Override
    public Attempt lastAttempt(String address) {
        return attempts.get(address);
    }

    @Override
    public void saveAttempt(String address, Attempt attempt) {
        attempts.put(address, attempt);
    }

    @Override
    public Set<String> unbondedByUs() {
        return new HashSet<>(unbondedByUs);
    }

    @Override
    public void setUnbondedByUs(String address, boolean value) {
        if (value) unbondedByUs.add(address); else unbondedByUs.remove(address);
    }

    @Override
    public Long lastAdvert(String address) {
        return adverts.get(address);
    }

    @Override
    public void saveAdvert(String address, long atMs) {
        adverts.put(address, atMs);
    }

    @Override
    public Long burstStart(String address) {
        return bursts.get(address);
    }

    @Override
    public void setBurstStart(String address, Long atMs) {
        if (atMs == null) bursts.remove(address); else bursts.put(address, atMs);
    }

    @Override
    public long waitUntil() {
        return waitUntil;
    }

    @Override
    public void setWaitUntil(long atMs) {
        waitUntil = atMs;
    }
}
