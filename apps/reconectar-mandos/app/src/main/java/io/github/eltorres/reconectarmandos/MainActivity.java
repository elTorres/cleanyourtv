package io.github.eltorres.reconectarmandos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressLint("MissingPermission")
public final class MainActivity extends Activity {
    private static final long REFRESH_MS = 2_000;
    private static final int PERMISSION_REQUEST = 1;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Long> lastSeen = new HashMap<>();
    private final Map<String, RowViews> rows = new LinkedHashMap<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            render();
            main.postDelayed(this, REFRESH_MS);
        }
    };
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            lastSeen.put(result.getDevice().getAddress(), System.currentTimeMillis());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
        }
    };

    private RemoteStore store;
    private AndroidBluetooth bluetooth;
    private BluetoothLeScanner scanner;
    private Set<String> scanned = new HashSet<>();
    private String rowsKey = "";
    private View missingBlock;
    private TextView missingText;
    private Button allowLocation;
    private TextView empty;
    private LinearLayout remotes;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        store = new PrefsRemoteStore(this);
        missingBlock = findViewById(R.id.missing);
        missingText = findViewById(R.id.missing_text);
        allowLocation = findViewById(R.id.allow_location);
        empty = findViewById(R.id.empty);
        remotes = findViewById(R.id.remotes);
        allowLocation.setOnClickListener(v -> requestPermissions(scanPermissions(), PERMISSION_REQUEST));
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (bluetooth == null || bluetooth.enabled() && !bluetooth.hasInput()) openBluetooth();
        main.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        main.removeCallbacks(refresh);
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetooth != null) bluetooth.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        render();
    }

    private void openBluetooth() {
        AndroidBluetooth previous = bluetooth;
        AndroidBluetooth.open(this, opened -> {
            if (previous != null) previous.close();
            bluetooth = opened;
            render();
        });
    }

    private void render() {
        if (bluetooth == null) return;
        List<Missing> missing = RemoteStatus.missing(hasScanPermissions(), locationOn(), bluetooth.enabled());
        renderMissing(missing);
        Map<String, String> marked = store.marked();
        List<Remote> listed = RemoteStatus.listed(bluetooth.bondedLe(), marked);
        empty.setVisibility(listed.isEmpty() ? View.VISIBLE : View.GONE);
        rebuildRowsIfChanged(listed, marked);
        for (Remote remote : listed) {
            RowViews row = rows.get(remote.address());
            boolean isMarked = marked.containsKey(remote.address());
            row.toggle.setText(getString(isMarked ? R.string.remote_marked : R.string.remote_not_marked,
                    remote.name(), remote.address()));
            row.status.setText(statusText(remote.address(), isMarked));
        }
        if (missing.isEmpty() && resumed) startScan(marked); else stopScan();
    }

    private void renderMissing(List<Missing> missing) {
        missingBlock.setVisibility(missing.isEmpty() ? View.GONE : View.VISIBLE);
        List<String> lines = new ArrayList<>();
        for (Missing item : missing) lines.add(getString(missingText(item)));
        missingText.setText(String.join("\n", lines));
        allowLocation.setVisibility(missing.contains(Missing.LOCATION_PERMISSION) ? View.VISIBLE : View.GONE);
    }

    private void rebuildRowsIfChanged(List<Remote> listed, Map<String, String> marked) {
        StringBuilder key = new StringBuilder();
        for (Remote remote : listed) key.append(remote.address()).append(remote.name()).append(';');
        if (key.toString().equals(rowsKey)) return;
        rowsKey = key.toString();
        View focused = getCurrentFocus();
        String focusedAddress = focused == null ? null : (String) focused.getTag();
        remotes.removeAllViews();
        rows.clear();
        for (Remote remote : listed) {
            RowViews row = new RowViews(remote);
            rows.put(remote.address(), row);
            remotes.addView(row.toggle);
            remotes.addView(row.status);
            if (remote.address().equals(focusedAddress)) row.toggle.requestFocus();
        }
    }

    private String statusText(String address, boolean isMarked) {
        if (!isMarked) return getString(R.string.remote_unmarked);
        List<String> parts = new ArrayList<>();
        parts.add(getString(stateText(RemoteStatus.of(address, bluetooth, false))));
        Long seen = lastSeen.get(address);
        if (seen != null) parts.add(getString(R.string.last_advert_seen, clock.format(new Date(seen))));
        Attempt attempt = store.lastAttempt(address);
        if (attempt != null) {
            String at = clock.format(new Date(attempt.atMs()));
            parts.add(attempt.connected() ? getString(R.string.last_attempt_ok, at)
                    : getString(R.string.last_attempt_failed, at, attempt.detail()));
        }
        return String.join(" · ", parts);
    }

    private void toggle(Remote remote) {
        if (store.marked().containsKey(remote.address())) store.unmark(remote.address());
        else store.mark(remote);
        render();
    }

    private void startScan(Map<String, String> marked) {
        if (scanner != null && scanned.equals(marked.keySet())) return;
        stopScan();
        if (marked.isEmpty() || bluetooth.adapter() == null) return;
        scanner = bluetooth.adapter().getBluetoothLeScanner();
        if (scanner == null) return;
        List<ScanFilter> filters = new ArrayList<>();
        for (String address : marked.keySet()) filters.add(new ScanFilter.Builder().setDeviceAddress(address).build());
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanner.startScan(filters, settings, scanCallback);
        scanned = new HashSet<>(marked.keySet());
    }

    private void stopScan() {
        if (scanner == null) return;
        try {
            scanner.stopScan(scanCallback);
        } catch (IllegalStateException ignored) {
        }
        scanner = null;
        scanned = new HashSet<>();
    }

    private boolean hasScanPermissions() {
        for (String permission : scanPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private static String[] scanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private boolean locationOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager location = getSystemService(LocationManager.class);
            return location != null && location.isLocationEnabled();
        }
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF;
    }

    private static int stateText(RemoteState state) {
        switch (state) {
            case CONNECTED:
                return R.string.remote_connected;
            case WAITING:
                return R.string.remote_waiting;
            case RECONNECTING:
                return R.string.remote_reconnecting;
            default:
                return R.string.remote_not_bonded;
        }
    }

    private static int missingText(Missing missing) {
        switch (missing) {
            case BLUETOOTH_OFF:
                return R.string.missing_bluetooth_off;
            case LOCATION_PERMISSION:
                return R.string.missing_location_permission;
            default:
                return R.string.missing_location_off;
        }
    }

    private final class RowViews {
        final Button toggle;
        final TextView status;

        RowViews(Remote remote) {
            toggle = new Button(MainActivity.this);
            toggle.setTag(remote.address());
            toggle.setAllCaps(false);
            toggle.setOnClickListener(v -> toggle(remote));
            LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            toggleParams.topMargin = dp(16);
            toggle.setLayoutParams(toggleParams);
            status = new TextView(MainActivity.this);
            status.setTextColor(getColor(R.color.muted));
            status.setTextSize(16);
            status.setPadding(dp(20), dp(4), 0, 0);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
