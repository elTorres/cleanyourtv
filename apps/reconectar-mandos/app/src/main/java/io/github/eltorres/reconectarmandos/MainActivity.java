package io.github.eltorres.reconectarmandos;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final long REFRESH_MS = 2_000;
    private static final int PERMISSION_REQUEST = 1;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, RowViews> rows = new LinkedHashMap<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            render();
            main.postDelayed(this, REFRESH_MS);
        }
    };

    private Services services;
    private String rowsKey = "";
    private View missingBlock;
    private TextView missingText;
    private Button allowLocation;
    private TextView empty;
    private LinearLayout remotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        missingBlock = findViewById(R.id.missing);
        missingText = findViewById(R.id.missing_text);
        allowLocation = findViewById(R.id.allow_location);
        empty = findViewById(R.id.empty);
        remotes = findViewById(R.id.remotes);
        allowLocation.setOnClickListener(v -> requestPermissions(
                Requirements.permissionsToRequest(Requirements.missing(this, services.bluetooth)), PERMISSION_REQUEST));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Services.with(this, ready -> {
            services = ready;
            services.watcher.start();
            main.removeCallbacks(refresh);
            main.post(refresh);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        main.removeCallbacks(refresh);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (services != null) services.watcher.start();
        render();
    }

    private void render() {
        if (services == null) return;
        renderMissing(Requirements.missing(this, services.bluetooth));
        Map<String, String> marked = services.store.marked();
        List<Remote> listed = RemoteStatus.listed(services.bluetooth.bondedLe(), marked);
        empty.setVisibility(listed.isEmpty() ? View.VISIBLE : View.GONE);
        rebuildRowsIfChanged(listed, marked);
        for (Remote remote : listed) {
            RowViews row = rows.get(remote.address());
            boolean isMarked = marked.containsKey(remote.address());
            row.toggle.setText(getString(isMarked ? R.string.remote_marked : R.string.remote_not_marked,
                    remote.name(), remote.address()));
            row.status.setText(statusText(remote.address(), isMarked));
        }
    }

    private void renderMissing(List<Missing> missing) {
        missingBlock.setVisibility(missing.isEmpty() ? View.GONE : View.VISIBLE);
        List<String> lines = new ArrayList<>();
        for (Missing item : missing) lines.add(getString(missingText(item)));
        missingText.setText(String.join("\n", lines));
        boolean permissionMissing = missing.contains(Missing.LOCATION_PERMISSION)
                || missing.contains(Missing.BACKGROUND_LOCATION);
        allowLocation.setVisibility(permissionMissing ? View.VISIBLE : View.GONE);
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
        boolean reconnecting = services.reconnector.reconnecting(address);
        parts.add(getString(stateText(RemoteStatus.of(address, services.bluetooth, reconnecting))));
        Long seen = services.store.lastAdvert(address);
        if (seen != null) parts.add(getString(R.string.last_advert_seen, clock.format(new Date(seen))));
        Attempt attempt = services.store.lastAttempt(address);
        if (attempt != null) {
            String at = clock.format(new Date(attempt.atMs()));
            parts.add(attempt.connected() ? getString(R.string.last_attempt_ok, at)
                    : getString(R.string.last_attempt_failed, at, attempt.detail()));
        }
        return String.join(" · ", parts);
    }

    private void toggle(Remote remote) {
        if (services.store.marked().containsKey(remote.address())) services.store.unmark(remote.address());
        else services.store.mark(remote);
        services.watcher.start();
        render();
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
            case BACKGROUND_LOCATION:
                return R.string.missing_background_location;
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
