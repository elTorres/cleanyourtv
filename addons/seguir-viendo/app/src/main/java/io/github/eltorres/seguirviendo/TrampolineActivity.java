package io.github.eltorres.seguirviendo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public final class TrampolineActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String target = Trampoline.targetOrDefault(getIntent().getStringExtra(WatchNextMapper.EXTRA_TARGET));
        String link = getIntent().getStringExtra(WatchNextMapper.EXTRA_LINK);
        PackageManager pm = getPackageManager();
        Intent linkIntent = link == null ? null : new Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(target);
        Intent appIntent = pm.getLeanbackLaunchIntentForPackage(target);
        if (appIntent == null) appIntent = pm.getLaunchIntentForPackage(target);
        boolean linkResolves = linkIntent != null && linkIntent.resolveActivity(pm) != null;
        switch (Trampoline.choose(linkResolves, appIntent != null)) {
            case OPEN_LINK:
                if (open(linkIntent) || open(appIntent)) return;
                break;
            case OPEN_APP:
                if (open(appIntent)) return;
                break;
            default:
                break;
        }
        showMissing(target);
    }

    private boolean open(Intent intent) {
        if (intent == null) return false;
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private void showMissing(String target) {
        setContentView(R.layout.activity_trampoline);
        ((TextView) findViewById(R.id.message)).setText(getString(R.string.target_missing, label(target)));
    }

    private String label(String target) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(target, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return Trampoline.STREMIO.equals(target) ? "Stremio" : target;
        }
    }
}
