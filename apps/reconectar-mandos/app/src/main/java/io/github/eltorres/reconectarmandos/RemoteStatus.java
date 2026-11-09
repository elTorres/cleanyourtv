package io.github.eltorres.reconectarmandos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RemoteStatus {
    private RemoteStatus() {
    }

    static RemoteState of(String address, BluetoothPort bluetooth, boolean reconnecting) {
        if (reconnecting) return RemoteState.RECONNECTING;
        if (bluetooth.inputConnected(address)) return RemoteState.CONNECTED;
        if (bluetooth.bonded(address)) return RemoteState.WAITING;
        return RemoteState.NOT_BONDED;
    }

    static List<Missing> missing(boolean locationPermission, boolean locationOn, boolean bluetoothOn) {
        List<Missing> missing = new ArrayList<>();
        if (!bluetoothOn) missing.add(Missing.BLUETOOTH_OFF);
        if (!locationPermission) missing.add(Missing.LOCATION_PERMISSION);
        if (!locationOn) missing.add(Missing.LOCATION_OFF);
        return missing;
    }

    static List<Remote> listed(List<Remote> bonded, Map<String, String> marked) {
        List<Remote> listed = new ArrayList<>();
        for (Map.Entry<String, String> entry : marked.entrySet()) {
            listed.add(new Remote(entry.getKey(), entry.getValue()));
        }
        for (Remote remote : bonded) {
            if (!marked.containsKey(remote.address())) listed.add(remote);
        }
        return listed;
    }
}
