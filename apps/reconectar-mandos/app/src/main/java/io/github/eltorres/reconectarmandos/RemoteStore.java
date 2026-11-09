package io.github.eltorres.reconectarmandos;

import java.util.Map;
import java.util.Set;

interface RemoteStore {
    Map<String, String> marked();

    void mark(Remote remote);

    void unmark(String address);

    Attempt lastAttempt(String address);

    void saveAttempt(String address, Attempt attempt);

    Set<String> unbondedByUs();

    void setUnbondedByUs(String address, boolean value);

    Long lastAdvert(String address);

    void saveAdvert(String address, long atMs);

    Long burstStart(String address);

    void setBurstStart(String address, Long atMs);

    long waitUntil();

    void setWaitUntil(long atMs);
}
