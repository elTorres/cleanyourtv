package io.github.eltorres.reconectarmandos;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressLint("MissingPermission")
final class AndroidBluetooth implements BluetoothPort {
    private static final int HID_HOST = 4;
    private static final int TRANSPORT_LE = 2;

    private final BluetoothAdapter adapter;
    private final BluetoothProfile input;

    private AndroidBluetooth(BluetoothAdapter adapter, BluetoothProfile input) {
        this.adapter = adapter;
        this.input = input;
    }

    static void open(Context context, Consumer<AndroidBluetooth> ready) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            ready.accept(new AndroidBluetooth(adapter, null));
            return;
        }
        boolean requested = adapter.getProfileProxy(context.getApplicationContext(), new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                ready.accept(new AndroidBluetooth(adapter, proxy));
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, HID_HOST);
        if (!requested) ready.accept(new AndroidBluetooth(adapter, null));
    }

    void close() {
        if (adapter != null && input != null) adapter.closeProfileProxy(HID_HOST, input);
    }

    BluetoothAdapter adapter() {
        return adapter;
    }

    boolean hasInput() {
        return input != null;
    }

    @Override
    public boolean enabled() {
        return adapter != null && adapter.isEnabled();
    }

    @Override
    public List<Remote> bondedLe() {
        List<Remote> remotes = new ArrayList<>();
        if (!enabled()) return remotes;
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) continue;
            remotes.add(new Remote(device.getAddress(), device.getName()));
        }
        remotes.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return remotes;
    }

    @Override
    public boolean bonded(String address) {
        return enabled() && adapter.getRemoteDevice(address).getBondState() == BluetoothDevice.BOND_BONDED;
    }

    @Override
    public boolean inputConnected(String address) {
        return enabled() && input != null
                && input.getConnectionState(adapter.getRemoteDevice(address)) == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public boolean removeBond(String address) {
        if (!enabled()) return false;
        try {
            return (Boolean) BluetoothDevice.class.getMethod("removeBond").invoke(adapter.getRemoteDevice(address));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    public boolean createBondLe(String address) {
        if (!enabled()) return false;
        try {
            return (Boolean) BluetoothDevice.class.getMethod("createBond", int.class)
                    .invoke(adapter.getRemoteDevice(address), TRANSPORT_LE);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
