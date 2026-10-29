package io.github.eltorres.seguirviendo;

import android.content.Context;

import java.util.List;

final class Services {
    private static Services instance;

    final SecretStore secrets;
    final Settings settings;
    final TraktAuth traktAuth;
    final SyncService sync;

    private Services(Context context) {
        Http http = new UrlConnectionHttp();
        secrets = new KeystoreSecretStore(context);
        settings = new Settings(context);
        traktAuth = new TraktAuth(http, secrets, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_CLIENT_SECRET,
                BuildConfig.TRAKT_REDIRECT_URI, System::currentTimeMillis);
        TraktSource trakt = new TraktSource(traktAuth, http, () -> settings.targetPackage(TraktPlayback.SOURCE_ID));
        sync = new SyncService(List.of(trakt), new TvWatchNextStore(context.getContentResolver()),
                System::currentTimeMillis);
    }

    static synchronized Services get(Context context) {
        if (instance == null) instance = new Services(context.getApplicationContext());
        return instance;
    }
}
