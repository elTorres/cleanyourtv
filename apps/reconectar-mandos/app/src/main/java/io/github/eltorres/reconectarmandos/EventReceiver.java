package io.github.eltorres.reconectarmandos;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public final class EventReceiver extends BroadcastReceiver {
    private static final String ACTION_RESUMED_FROM_STANDBY = "tcl.sys.intent.action.STR_RESUME_COMPLETED";
    private static final long BLUETOOTH_RETRY_MS = 60_000;
    private static final long[] POWER_ON_RECHECK_MS = {3_000, 6_000, 10_000, 15_000, 20_000, 30_000};
    private static final long[] CONNECTION_RECHECK_MS = {2_000, 5_000, 8_000};

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Context app = context.getApplicationContext();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || ACTION_RESUMED_FROM_STANDBY.equals(action)) {
            PendingResult pending = goAsync();
            Services.with(app, services -> {
                services.watcher.start();
                recheck(app, pending, POWER_ON_RECHECK_MS, 0);
            });
        } else if (AndroidAdvertWatch.ACTION_CHECK.equals(action)) {
            PendingResult pending = goAsync();
            Services.with(app, services -> {
                services.watcher.refresh();
                retryWhileBluetoothIsOff(services);
                pending.finish();
            });
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)
                || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;
            PendingResult pending = goAsync();
            Services.with(app, services -> {
                if (!services.store.marked().containsKey(device.getAddress())) {
                    pending.finish();
                    return;
                }
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) services.watcher.start();
                else services.watcher.refresh();
                recheck(app, pending, CONNECTION_RECHECK_MS, 0);
            });
        }
    }

    private static void recheck(Context app, PendingResult pending, long[] delays, int index) {
        if (index == delays.length) {
            Services.with(app, services -> {
                retryWhileBluetoothIsOff(services);
                pending.finish();
            });
            return;
        }
        long delay = delays[index] - (index == 0 ? 0 : delays[index - 1]);
        new Handler(Looper.getMainLooper()).postDelayed(() -> Services.with(app, services -> {
            services.watcher.refresh();
            recheck(app, pending, delays, index + 1);
        }), delay);
    }

    private static void retryWhileBluetoothIsOff(Services services) {
        if (!services.bluetooth.enabled() && services.watcher.waiting()) {
            services.watch.checkAt(System.currentTimeMillis() + BLUETOOTH_RETRY_MS);
        }
    }
}
