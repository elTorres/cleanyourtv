package io.github.eltorres.seguirviendo;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OptionsActivity extends Activity {
    static final String EXTRA_PROGRAM_ID = "com.gathin.tvhome.extra.WATCH_NEXT_PROGRAM_ID";
    private static final int DROP_IN_MS = 220;
    private static final int CARD_GAP_DP = 10;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private HideService hide;
    private boolean chosen;

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
        setContentView(R.layout.options_menu);
        View screen = findViewById(android.R.id.content);
        LinearLayout panel = findViewById(R.id.panel);
        for (HideScope scope : hide.options(row)) addRow(panel, label(row, scope), () -> hide(row, scope));
        screen.post(() -> dropIn(screen, panel));
    }

    private void addRow(LinearLayout rows, String title, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.options_row, rows, false);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        row.setOnClickListener(v -> choose(action));
        boolean[] freshPress = {false};
        row.setOnKeyListener((v, code, event) -> {
            if (!isConfirm(code)) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) freshPress[0] = true;
                return true;
            }
            boolean activate = freshPress[0];
            freshPress[0] = false;
            if (activate) choose(action);
            return true;
        });
        rows.addView(row);
    }

    private static boolean isConfirm(int code) {
        return code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private void choose(Runnable action) {
        if (chosen) return;
        chosen = true;
        findViewById(R.id.panel).setVisibility(View.INVISIBLE);
        action.run();
    }

    private void dropIn(View screen, ViewGroup panel) {
        float density = getResources().getDisplayMetrics().density;
        MenuPlacement placement = placement(screen, panel, Math.round(CARD_GAP_DP * density));
        panel.setX(placement.x());
        panel.setY(placement.y() + (placement.above() ? 8 : -8) * density);
        panel.setAlpha(0f);
        panel.setPivotY(placement.above() ? panel.getHeight() : 0f);
        panel.setScaleY(0.92f);
        panel.setVisibility(View.VISIBLE);
        panel.animate().alpha(1f).scaleY(1f).y(placement.y()).setDuration(DROP_IN_MS)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        if (panel.getChildCount() > 0) panel.getChildAt(0).requestFocus();
    }

    private MenuPlacement placement(View screen, View panel, int gap) {
        Rect card = getIntent().getSourceBounds();
        if (card == null) {
            return MenuPlacement.centered(panel.getWidth(), panel.getHeight(), screen.getWidth(), screen.getHeight());
        }
        int[] origin = new int[2];
        screen.getLocationOnScreen(origin);
        boolean rtl = screen.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        return MenuPlacement.nextTo(card.left - origin[0], card.top - origin[1], card.right - origin[0],
                card.bottom - origin[1], panel.getWidth(), panel.getHeight(), screen.getWidth(), screen.getHeight(),
                gap, rtl);
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
