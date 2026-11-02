package io.github.eltorres.seguirviendo;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Services {
    private static Services instance;

    final SecretStore secrets;
    final Settings settings;
    final TraktAuth traktAuth;
    final MovistarAuth movistarAuth;
    final Posters posters;
    final boolean tmdbPosters;
    final SyncService sync;
    final SourcesService sources;

    private Services(Context context) {
        Http http = new UrlConnectionHttp();
        secrets = new KeystoreSecretStore(context);
        settings = new Settings(context);
        traktAuth = new TraktAuth(http, secrets, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_CLIENT_SECRET,
                BuildConfig.TRAKT_REDIRECT_URI, System::currentTimeMillis);
        movistarAuth = new MovistarAuth(http, secrets, System::currentTimeMillis);
        TraktSource trakt = new TraktSource(traktAuth, http, () -> settings.targetPackage(TraktPlayback.SOURCE_ID),
                System::currentTimeMillis);
        ArtworkLookup tmdb = TmdbArtwork.orNull(http, BuildConfig.TMDB_API_KEY, Locale.getDefault().toLanguageTag());
        tmdbPosters = tmdb != null;
        Map<String, ArtworkLookup> artwork = new HashMap<>();
        if (tmdb != null) {
            artwork.put(Posters.TV, tmdb);
            artwork.put(Posters.MOVIE, tmdb);
        }
        artwork.put(MovistarViewings.IMAGE, new MovistarArtwork(http));
        posters = new Posters(new FilePosterStore(PosterProvider.dir(context), PosterProvider.AUTHORITY),
                new RoutedArtwork(artwork));
        WatchNextStore store = new TvWatchNextStore(context.getContentResolver());
        MovistarSource movistar = new MovistarSource(movistarAuth, http, System::currentTimeMillis);
        sync = new SyncService(List.of(trakt, movistar), store, posters, System::currentTimeMillis);
        sources = new SourcesService(Map.of(TraktPlayback.SOURCE_ID, traktAuth::disconnect,
                MovistarAuth.SOURCE_ID, movistarAuth::disconnect), store, settings);
    }

    static synchronized Services get(Context context) {
        if (instance == null) instance = new Services(context.getApplicationContext());
        return instance;
    }
}
