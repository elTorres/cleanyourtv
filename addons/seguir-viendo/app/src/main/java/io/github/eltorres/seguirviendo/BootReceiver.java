package io.github.eltorres.seguirviendo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (Services.get(context).traktAuth.state() != SourceState.CONNECTED) return;
        SyncScheduler.schedule(context);
        SyncScheduler.syncSoon(context);
    }
}
