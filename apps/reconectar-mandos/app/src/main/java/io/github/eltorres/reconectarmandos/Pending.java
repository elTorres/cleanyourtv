package io.github.eltorres.reconectarmandos;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class Pending {
    private Pending() {
    }

    static Set<String> of(Map<String, String> marked, Set<String> unbondedByUs, BluetoothPort bluetooth) {
        Set<String> pending = new LinkedHashSet<>();
        for (String address : marked.keySet()) {
            if (bluetooth.inputConnected(address)) return new LinkedHashSet<>();
            if (bluetooth.bonded(address) || unbondedByUs.contains(address)) pending.add(address);
        }
        return pending;
    }
}
