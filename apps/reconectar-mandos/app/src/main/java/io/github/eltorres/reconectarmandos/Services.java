package io.github.eltorres.reconectarmandos;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class Services {
    private static Services instance;
    private static List<Consumer<Services>> waiting;

    final RemoteStore store;
    final AndroidBluetooth bluetooth;
    final AndroidAdvertWatch watch;
    final Reconnector reconnector;
    final Watcher watcher;

    private Services(Context context, AndroidBluetooth bluetooth) {
        store = new PrefsRemoteStore(context);
        this.bluetooth = bluetooth;
        watch = new AndroidAdvertWatch(context, bluetooth);
        reconnector = new Reconnector(store, bluetooth, () -> Requirements.canWatch(context, bluetooth));
        watcher = new Watcher(store, bluetooth, watch,
                () -> Requirements.canWatch(context, bluetooth), System::currentTimeMillis);
    }

    static void with(Context context, Consumer<Services> ready) {
        if (instance != null && !needsReopen(instance.bluetooth)) {
            ready.accept(instance);
            return;
        }
        if (waiting != null) {
            waiting.add(ready);
            return;
        }
        waiting = new ArrayList<>();
        waiting.add(ready);
        Context app = context.getApplicationContext();
        AndroidBluetooth.open(app, bluetooth -> {
            if (instance != null) instance.bluetooth.close();
            instance = new Services(app, bluetooth);
            List<Consumer<Services>> callbacks = waiting;
            waiting = null;
            for (Consumer<Services> callback : callbacks) callback.accept(instance);
        });
    }

    private static boolean needsReopen(AndroidBluetooth bluetooth) {
        return bluetooth.enabled() && !bluetooth.hasInput();
    }
}
