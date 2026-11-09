package io.github.eltorres.reconectarmandos;

import java.util.Map;

interface RemoteStore {
    Map<String, String> marked();

    void mark(Remote remote);

    void unmark(String address);

    Attempt lastAttempt(String address);
}
