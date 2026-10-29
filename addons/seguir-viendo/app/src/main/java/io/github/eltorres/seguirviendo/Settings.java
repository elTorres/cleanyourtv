package io.github.eltorres.seguirviendo;

import android.content.Context;
import android.content.SharedPreferences;

final class Settings {
    private final SharedPreferences prefs;

    Settings(Context context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    String targetPackage(String sourceId) {
        return Trampoline.targetOrDefault(prefs.getString("target." + sourceId, null));
    }

    long lastSyncMs() {
        return prefs.getLong("last_sync", 0);
    }

    void setLastSyncMs(long value) {
        prefs.edit().putLong("last_sync", value).apply();
    }
}
