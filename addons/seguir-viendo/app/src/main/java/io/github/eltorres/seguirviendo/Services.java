package io.github.eltorres.seguirviendo;

import android.content.Context;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Services {
    private static Services instance;

    final SecretStore secrets;
    final Settings settings;
    final TraktAuth traktAuth;
    final Posters posters;
    final SyncService sync;
    final SourcesService sources;

    private Services(Context context) {
        Http http = new UrlConnectionHttp();
        secrets = new KeystoreSecretStore(context);
        settings = new Settings(context);
        traktAuth = new TraktAuth(http, secrets, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_CLIENT_SECRET,
                BuildConfig.TRAKT_REDIRECT_URI, System::currentTimeMillis);
        TraktSource trakt = new TraktSource(traktAuth, http, () -> settings.targetPackage(TraktPlayback.SOURCE_ID),
                System::currentTimeMillis);
        posters = new Posters(new FilePosterStore(PosterProvider.dir(context), PosterProvider.AUTHORITY),
                TmdbArtwork.orNull(http, BuildConfig.TMDB_API_KEY, Locale.getDefault().toLanguageTag()));
        WatchNextStore store = new TvWatchNextStore(context.getContentResolver());
        sync = new SyncService(List.of(trakt), store, posters, System::currentTimeMillis);
        sources = new SourcesService(Map.of(TraktPlayback.SOURCE_ID, traktAuth::disconnect), store, settings);
    }

    static synchronized Services get(Context context) {
        if (instance == null) instance = new Services(context.getApplicationContext());
        return instance;
    }
}
