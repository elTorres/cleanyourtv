package io.github.eltorres.seguirviendo;

import android.content.Context;
import android.content.SharedPreferences;

final class Settings implements Targets {
    private final SharedPreferences prefs;

    Settings(Context context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Override
    public String targetPackage(String sourceId) {
        return Trampoline.targetOrDefault(prefs.getString("target." + sourceId, null));
    }

    @Override
    public void setTargetPackage(String sourceId, String packageName) {
        prefs.edit().putString("target." + sourceId, packageName).apply();
    }

    long lastSyncMs() {
        return prefs.getLong("last_sync", 0);
    }

    void setLastSyncMs(long value) {
        prefs.edit().putLong("last_sync", value).apply();
    }
}
