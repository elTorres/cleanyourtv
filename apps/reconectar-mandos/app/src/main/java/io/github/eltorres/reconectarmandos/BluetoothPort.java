package io.github.eltorres.reconectarmandos;

import java.util.List;

interface BluetoothPort {
    boolean enabled();

    List<Remote> bondedLe();

    boolean bonded(String address);

    boolean inputConnected(String address);
}
