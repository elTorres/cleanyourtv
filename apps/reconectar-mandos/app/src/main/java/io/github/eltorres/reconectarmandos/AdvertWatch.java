package io.github.eltorres.reconectarmandos;

import java.util.Set;

interface AdvertWatch {
    Set<String> watching();

    void watch(Set<String> addresses, long untilMs);

    void stop();
}
