package io.github.eltorres.reconectarmandos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class FakeAdvertWatch implements AdvertWatch {
    final List<String> calls = new ArrayList<>();
    private Set<String> watching = new HashSet<>();

    @Override
    public Set<String> watching() {
        return new HashSet<>(watching);
    }

    @Override
    public void watch(Set<String> addresses, long untilMs) {
        calls.add("watch " + new TreeSet<>(addresses) + " until " + untilMs);
        watching = new HashSet<>(addresses);
    }

    @Override
    public void stop() {
        calls.add("stop");
        watching = new HashSet<>();
    }
}
