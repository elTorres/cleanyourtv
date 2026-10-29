package io.github.eltorres.seguirviendo;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final long SLOW_DOWN_MS = 5_000;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Services services;
    private TextView traktState;
    private TextView traktCode;
    private Button traktConnect;
    private Button syncNow;
    private Button disconnect;
    private Button target;
    private TextView posters;
    private TextView lastSync;
    private TraktAuth.DeviceCode pending;
    private long pollIntervalMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        services = Services.get(this);
        traktState = findViewById(R.id.trakt_state);
        traktCode = findViewById(R.id.trakt_code);
        traktConnect = findViewById(R.id.trakt_connect);
        syncNow = findViewById(R.id.sync_now);
        disconnect = findViewById(R.id.disconnect);
        target = findViewById(R.id.target_app);
        posters = findViewById(R.id.posters);
        lastSync = findViewById(R.id.last_sync);
        traktConnect.setOnClickListener(v -> requestCode());
        syncNow.setOnClickListener(v -> syncAll());
        disconnect.setOnClickListener(v -> disconnectTrakt());
        target.setOnClickListener(v -> nextTarget());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        main.removeCallbacksAndMessages(null);
        pending = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }

    private void render() {
        SourceState state = services.traktAuth.state();
        traktState.setText(stateText(state));
        traktConnect.setEnabled(state != SourceState.UNAVAILABLE);
        traktConnect.setText(state == SourceState.CONNECTED ? R.string.trakt_reconnect : R.string.trakt_connect);
        syncNow.setEnabled(state == SourceState.CONNECTED);
        boolean signedIn = state == SourceState.CONNECTED || state == SourceState.RECONNECT_NEEDED;
        disconnect.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        target.setText(getString(R.string.target_app, label(currentTarget())));
        posters.setText(services.posters.available() ? R.string.posters_on : R.string.posters_off);
        if (state == SourceState.CONNECTED) SyncScheduler.schedule(this);
        long last = services.settings.lastSyncMs();
        lastSync.setText(last == 0 ? "" : getString(R.string.last_sync,
                DateFormat.getTimeFormat(this).format(new Date(last))));
        if (pending == null) traktCode.setText("");
    }

    private int stateText(SourceState state) {
        switch (state) {
            case UNAVAILABLE:
                return R.string.trakt_unavailable;
            case CONNECTED:
                return R.string.state_connected;
            case RECONNECT_NEEDED:
                return R.string.reconnect_needed;
            default:
                return R.string.state_disconnected;
        }
    }

    private void requestCode() {
        traktConnect.setEnabled(false);
        worker.execute(() -> {
            try {
                TraktAuth.DeviceCode code = services.traktAuth.requestCode();
                main.post(() -> showCode(code));
            } catch (IOException e) {
                Logs.w(services.secrets, "Trakt device code failed", e);
                main.post(() -> {
                    traktCode.setText(R.string.network_error);
                    traktConnect.setEnabled(true);
                });
            }
        });
    }

    private void showCode(TraktAuth.DeviceCode code) {
        pending = code;
        pollIntervalMs = code.intervalMs;
        traktCode.setText(getString(R.string.trakt_code, code.verificationUrl, code.userCode));
        main.postDelayed(this::poll, pollIntervalMs);
    }

    private void poll() {
        TraktAuth.DeviceCode code = pending;
        if (code == null) return;
        worker.execute(() -> {
            TraktAuth.PollStatus status;
            try {
                status = services.traktAuth.poll(code).status;
            } catch (IOException e) {
                Logs.w(services.secrets, "Trakt device token failed", e);
                status = TraktAuth.PollStatus.PENDING;
            }
            TraktAuth.PollStatus result = status;
            main.post(() -> onPoll(code, result));
        });
    }

    private void onPoll(TraktAuth.DeviceCode code, TraktAuth.PollStatus status) {
        if (pending != code) return;
        switch (status) {
            case CONNECTED:
                pending = null;
                render();
                syncAll();
                return;
            case EXPIRED:
            case DENIED:
            case INVALID:
                pending = null;
                render();
                traktCode.setText(R.string.trakt_expired);
                return;
            case SLOW_DOWN:
                pollIntervalMs += SLOW_DOWN_MS;
                break;
            default:
                break;
        }
        main.postDelayed(this::poll, pollIntervalMs);
    }

    private void disconnectTrakt() {
        disconnect.setEnabled(false);
        worker.execute(() -> {
            services.sources.disconnect(TraktPlayback.SOURCE_ID);
            SyncScheduler.cancel(this);
            services.posters.retainOnly(List.of());
            main.post(() -> {
                disconnect.setEnabled(true);
                render();
            });
        });
    }

    private String currentTarget() {
        return services.settings.targetPackage(TraktPlayback.SOURCE_ID);
    }

    private void nextTarget() {
        PackageManager pm = getPackageManager();
        String next = Trampoline.nextTarget(currentTarget(), pkg -> pm.getLeanbackLaunchIntentForPackage(pkg) != null
                || pm.getLaunchIntentForPackage(pkg) != null);
        services.sources.setTargetPackage(TraktPlayback.SOURCE_ID, next);
        render();
        if (services.traktAuth.state() == SourceState.CONNECTED) syncAll();
    }

    private String label(String pkg) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void syncAll() {
        syncNow.setEnabled(false);
        worker.execute(() -> {
            SyncReport report = services.sync.syncAll();
            services.settings.setLastSyncMs(report.finishedAtMs());
            boolean failed = report.outcomes().containsValue(SyncReport.Outcome.NETWORK_ERROR);
            main.post(() -> {
                render();
                if (failed) lastSync.append("\n" + getString(R.string.network_error));
            });
        });
    }
}
