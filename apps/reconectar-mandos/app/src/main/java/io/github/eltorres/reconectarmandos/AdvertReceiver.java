package io.github.eltorres.reconectarmandos;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public final class AdvertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0) != 0) return;
        List<ScanResult> results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        if (results == null || results.isEmpty()) return;
        PendingResult pending = goAsync();
        Services.with(context, services -> {
            long now = System.currentTimeMillis();
            for (ScanResult result : results) {
                String address = result.getDevice().getAddress();
                Reconnector.Step step = services.reconnector.onAdvert(address, now);
                if (step != Reconnector.Step.IGNORE) ReconnectService.start(context, address, step);
            }
            services.watcher.refresh();
            pending.finish();
        });
    }
}
