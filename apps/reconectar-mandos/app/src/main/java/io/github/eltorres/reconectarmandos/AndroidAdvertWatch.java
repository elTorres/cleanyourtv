package io.github.eltorres.reconectarmandos;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressLint("MissingPermission")
final class AndroidAdvertWatch implements AdvertWatch {
    static final String ACTION_CHECK = "io.github.eltorres.reconectarmandos.CHECK";
    private static final long CHECK_WINDOW_MS = 60_000L;
    private static final long CHECK_GRACE_MS = 1_000L;
    private static final String FILE = "watch";
    private static final String WATCHING = "watching";

    private final Context context;
    private final AndroidBluetooth bluetooth;
    private final SharedPreferences prefs;

    AndroidAdvertWatch(Context context, AndroidBluetooth bluetooth) {
        this.context = context.getApplicationContext();
        this.bluetooth = bluetooth;
        prefs = this.context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public Set<String> watching() {
        return new HashSet<>(prefs.getStringSet(WATCHING, new HashSet<>()));
    }

    @Override
    public void watch(Set<String> addresses, long untilMs) {
        BluetoothLeScanner scanner = scanner();
        if (scanner == null) return;
        stopQuietly(scanner);
        List<ScanFilter> filters = new ArrayList<>();
        for (String address : addresses) filters.add(new ScanFilter.Builder().setDeviceAddress(address).build());
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        boolean started = scanner.startScan(filters, settings, scanIntent()) == 0;
        prefs.edit().putStringSet(WATCHING, started ? new HashSet<>(addresses) : new HashSet<>()).apply();
        if (started) checkAt(untilMs + CHECK_GRACE_MS);
    }

    @Override
    public void stop() {
        BluetoothLeScanner scanner = scanner();
        if (scanner != null) stopQuietly(scanner);
        alarms().cancel(checkIntent());
        prefs.edit().putStringSet(WATCHING, new HashSet<>()).apply();
    }

    void checkAt(long atMs) {
        alarms().setWindow(AlarmManager.RTC, atMs, CHECK_WINDOW_MS, checkIntent());
    }

    private BluetoothLeScanner scanner() {
        return bluetooth.enabled() ? bluetooth.adapter().getBluetoothLeScanner() : null;
    }

    private void stopQuietly(BluetoothLeScanner scanner) {
        try {
            scanner.stopScan(scanIntent());
        } catch (IllegalStateException ignored) {
        }
    }

    private AlarmManager alarms() {
        return context.getSystemService(AlarmManager.class);
    }

    private PendingIntent scanIntent() {
        int mutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        return PendingIntent.getBroadcast(context, 0, new Intent(context, AdvertReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | mutable);
    }

    private PendingIntent checkIntent() {
        return PendingIntent.getBroadcast(context, 1,
                new Intent(context, EventReceiver.class).setAction(ACTION_CHECK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
