package io.github.eltorres.seguirviendo;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class TraktSource implements ViewingSource {
    private final TraktAuth auth;
    private final Http http;
    private final Supplier<String> targetPackage;
    private final LongSupplier clock;

    TraktSource(TraktAuth auth, Http http, Supplier<String> targetPackage, LongSupplier clock) {
        this.auth = auth;
        this.http = http;
        this.targetPackage = targetPackage;
        this.clock = clock;
    }

    @Override
    public String id() {
        return TraktPlayback.SOURCE_ID;
    }

    @Override
    public SourceState state() {
        return auth.state();
    }

    @Override
    public List<ViewingItem> fetch() throws IOException, AuthException {
        auth.refreshIfNeeded();
        String target = targetPackage.get();
        try {
            List<ViewingItem> items = new ArrayList<>(TraktPlayback.parse(get("/sync/playback?extended=full"), target));
            Set<String> paused = new HashSet<>();
            for (ViewingItem item : items) paused.add(item.showKey());
            String watched = get("/sync/watched/shows?extended=noseasons");
            for (TraktProgress.WatchedShow show : TraktProgress.recentShows(watched, clock.getAsLong())) {
                if (paused.contains(show.showKey())) continue;
                ViewingItem next = TraktProgress.next(show, get("/shows/" + show.traktId + "/progress/watched"), target);
                if (next != null) items.add(next);
            }
            return items;
        } catch (JSONException e) {
            throw new IOException("Trakt: unreadable answer", e);
        }
    }

    private String get(String path) throws IOException, AuthException {
        Http.Response response = http.send("GET", TraktAuth.API + path, auth.apiHeaders(), null);
        if (response.code == 401 || response.code == 403) throw auth.rejected();
        if (!response.ok()) throw new IOException("Trakt " + path + ": HTTP " + response.code);
        return response.body;
    }
}
