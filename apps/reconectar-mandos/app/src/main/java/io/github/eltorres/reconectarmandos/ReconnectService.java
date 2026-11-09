package io.github.eltorres.reconectarmandos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class ReconnectService extends Service {
    private static final String EXTRA_ADDRESS = "address";
    private static final String EXTRA_STEP = "step";
    private static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";
    private static final String CHANNEL = "reconnect";
    private static final int NOTIFICATION = 1;
    private static final long TIMEOUT_MS = 35_000;
    private static final long POLL_MS = 1_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (services != null && bondRequested && services.bluetooth.inputConnected(address)) finish(true, "");
            else main.postDelayed(this, POLL_MS);
        }
    };
    private final Runnable timeout = this::timedOut;
    private final BroadcastReceiver bondChanges = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (services == null || device == null || !device.getAddress().equals(address)) return;
            if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) != BluetoothDevice.BOND_NONE) return;
            if (unbondRequested) {
                unbondRequested = false;
                if (services.reconnector.onUnbonded(address) == Reconnector.Step.BOND) bond();
                else finish(false, "unbonded");
            } else if (bondRequested) {
                finish(false, "bond failed, reason " + intent.getIntExtra(EXTRA_REASON, -1));
            }
        }
    };

    private Services services;
    private String address;
    private boolean unbondRequested;
    private boolean bondRequested;
    private boolean finished;

    static void start(Context context, String address, Reconnector.Step step) {
        context.startForegroundService(new Intent(context, ReconnectService.class)
                .putExtra(EXTRA_ADDRESS, address)
                .putExtra(EXTRA_STEP, step.name()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION, notification(intent == null ? null : intent.getStringExtra(EXTRA_ADDRESS)));
        if (address != null || intent == null || intent.getStringExtra(EXTRA_ADDRESS) == null) {
            if (address == null) stopSelf();
            return START_NOT_STICKY;
        }
        address = intent.getStringExtra(EXTRA_ADDRESS);
        Reconnector.Step step = Reconnector.Step.valueOf(intent.getStringExtra(EXTRA_STEP));
        registerReceiver(bondChanges, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        main.postDelayed(timeout, TIMEOUT_MS);
        Services.with(this, ready -> {
            services = ready;
            if (step == Reconnector.Step.UNBOND) unbond();
            else bond();
            main.post(poll);
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        main.removeCallbacks(poll);
        main.removeCallbacks(timeout);
        super.onDestroy();
    }

    private void timedOut() {
        finish(services != null && services.bluetooth.inputConnected(address), "timed out");
    }

    private void unbond() {
        unbondRequested = true;
        if (!services.bluetooth.removeBond(address)) finish(false, "unbond refused");
    }

    private void bond() {
        bondRequested = true;
        if (!services.bluetooth.createBondLe(address)) finish(false, "bond refused");
    }

    private void finish(boolean connected, String detail) {
        if (finished) return;
        finished = true;
        main.removeCallbacks(poll);
        main.removeCallbacks(timeout);
        unregisterReceiver(bondChanges);
        Services.with(this, ready -> {
            ready.reconnector.onFinished(address, connected, detail, System.currentTimeMillis());
            ready.watcher.refresh();
            stopForeground(true);
            stopSelf();
        });
    }

    private Notification notification(String address) {
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,
                getString(R.string.reconnect_channel), NotificationManager.IMPORTANCE_LOW));
        String name = address == null ? "" : new PrefsRemoteStore(this).marked().getOrDefault(address, address);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.reconnect_notification_title))
                .setContentText(getString(R.string.reconnect_notification_text, name))
                .setOngoing(true)
                .build();
    }
}
