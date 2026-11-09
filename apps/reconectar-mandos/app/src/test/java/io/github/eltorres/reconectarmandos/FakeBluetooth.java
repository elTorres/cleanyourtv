package io.github.eltorres.reconectarmandos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FakeBluetooth implements BluetoothPort {
    boolean enabled = true;
    final List<Remote> bonded = new ArrayList<>();
    final Set<String> connected = new HashSet<>();

    FakeBluetooth bond(String address, String name) {
        bonded.add(new Remote(address, name));
        return this;
    }

    FakeBluetooth connect(String address) {
        connected.add(address);
        return this;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<Remote> bondedLe() {
        return enabled ? new ArrayList<>(bonded) : new ArrayList<>();
    }

    @Override
    public boolean bonded(String address) {
        return enabled && bonded.stream().anyMatch(remote -> remote.address().equals(address));
    }

    @Override
    public boolean inputConnected(String address) {
        return enabled && connected.contains(address);
    }
}
