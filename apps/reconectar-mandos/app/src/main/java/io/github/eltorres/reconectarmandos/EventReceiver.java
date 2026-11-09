package io.github.eltorres.reconectarmandos;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public final class EventReceiver extends BroadcastReceiver {
    private static final long BLUETOOTH_RETRY_MS = 60_000;
    private static final long[] RECHECK_MS = {2_000, 5_000, 8_000};

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || AndroidAdvertWatch.ACTION_CHECK.equals(action)) {
            boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action);
            PendingResult pending = goAsync();
            Services.with(context, services -> {
                if (boot) services.watcher.start();
                else services.watcher.refresh();
                if (!services.bluetooth.enabled() && services.watcher.waiting()) {
                    services.watch.checkAt(System.currentTimeMillis() + BLUETOOTH_RETRY_MS);
                }
                pending.finish();
            });
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)
                || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;
            PendingResult pending = goAsync();
            Services.with(context, services -> {
                if (!services.store.marked().containsKey(device.getAddress())) {
                    pending.finish();
                    return;
                }
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) services.watcher.start();
                else services.watcher.refresh();
                recheck(services, pending, 0);
            });
        }
    }

    private static void recheck(Services services, PendingResult pending, int index) {
        if (index == RECHECK_MS.length) {
            pending.finish();
            return;
        }
        long delay = RECHECK_MS[index] - (index == 0 ? 0 : RECHECK_MS[index - 1]);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            services.watcher.refresh();
            recheck(services, pending, index + 1);
        }, delay);
    }
}
