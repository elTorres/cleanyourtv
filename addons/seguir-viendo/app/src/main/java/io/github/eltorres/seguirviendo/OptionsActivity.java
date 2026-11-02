package io.github.eltorres.seguirviendo;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OptionsActivity extends Activity {
    static final String EXTRA_PROGRAM_ID = "com.gathin.tvhome.extra.WATCH_NEXT_PROGRAM_ID";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private HideService hide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hide = Services.get(this).hide;
        long rowId = getIntent().getLongExtra(EXTRA_PROGRAM_ID, -1);
        worker.execute(() -> {
            PublishedRow row = rowId < 0 ? null : hide.row(rowId);
            main.post(() -> show(row));
        });
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }

    private void show(PublishedRow row) {
        if (isFinishing() || isDestroyed()) return;
        if (row == null) {
            Toast.makeText(this, R.string.entry_gone, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        List<HideScope> scopes = hide.options(row);
        CharSequence[] labels = new CharSequence[scopes.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = label(row, scopes.get(i));
        boolean[] freshPress = {false};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(row.title())
                .setItems(labels, (dialog, which) -> hide(row, scopes.get(which)))
                .setOnCancelListener(dialog -> finish())
                .setOnKeyListener((dialog, code, event) -> heldFromTheLauncher(code, event, freshPress))
                .show();
    }

    private static boolean heldFromTheLauncher(int code, KeyEvent event, boolean[] freshPress) {
        boolean confirm = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (!confirm) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) freshPress[0] = true;
        return !freshPress[0];
    }

    private void hide(PublishedRow row, HideScope scope) {
        worker.execute(() -> {
            hide.hide(row, scope);
            main.post(this::finish);
        });
    }

    private String label(PublishedRow row, HideScope scope) {
        switch (scope) {
            case EPISODE:
                return getString(R.string.hide_episode);
            case SEASON:
                return getString(R.string.hide_season, row.season());
            default:
                return getString(row.episode() == null ? R.string.hide_title : R.string.hide_show);
        }
    }
}
