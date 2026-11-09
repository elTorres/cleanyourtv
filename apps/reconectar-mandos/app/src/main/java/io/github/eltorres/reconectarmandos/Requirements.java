package io.github.eltorres.reconectarmandos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import java.util.List;

final class Requirements {
    private Requirements() {
    }

    static List<Missing> missing(Context context, BluetoothPort bluetooth) {
        return RemoteStatus.missing(hasScanPermissions(context), locationOn(context), bluetooth.enabled());
    }

    static String[] scanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private static boolean hasScanPermissions(Context context) {
        for (String permission : scanPermissions()) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private static boolean locationOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager location = context.getSystemService(LocationManager.class);
            return location != null && location.isLocationEnabled();
        }
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF;
    }
}
