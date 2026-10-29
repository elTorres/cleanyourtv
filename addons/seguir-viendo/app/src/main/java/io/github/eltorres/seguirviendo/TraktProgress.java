package io.github.eltorres.seguirviendo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class TraktProgress {
    static final long RECENT_MS = 60L * 24 * 60 * 60 * 1000;
    static final int MAX_SHOWS = 10;

    static final class WatchedShow {
        final String traktId;
        final String title;
        final String imdbId;
        final String tmdbId;
        final long lastWatchedMs;

        WatchedShow(String traktId, String title, String imdbId, String tmdbId, long lastWatchedMs) {
            this.traktId = traktId;
            this.title = title;
            this.imdbId = imdbId;
            this.tmdbId = tmdbId;
            this.lastWatchedMs = lastWatchedMs;
        }

        String showKey() {
            return "show:" + traktId;
        }
    }

    private TraktProgress() {
    }

    static List<WatchedShow> recentShows(String json, long nowMs) throws JSONException {
        JSONArray entries = new JSONArray(json);
        List<WatchedShow> shows = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            JSONObject show = entry.optJSONObject("show");
            String traktId = TraktJson.id(show, "trakt");
            long watched = TraktJson.instant(entry.optString("last_watched_at", null));
            if (traktId == null || nowMs - watched > RECENT_MS) continue;
            shows.add(new WatchedShow(traktId, show.optString("title"), TraktJson.id(show, "imdb"),
                    TraktJson.id(show, "tmdb"), watched));
        }
        shows.sort(Comparator.comparingLong((WatchedShow s) -> s.lastWatchedMs).reversed());
        return shows.size() > MAX_SHOWS ? new ArrayList<>(shows.subList(0, MAX_SHOWS)) : shows;
    }

    static ViewingItem next(WatchedShow show, String progressJson, String targetPackage) throws JSONException {
        JSONObject next = new JSONObject(progressJson).optJSONObject("next_episode");
        if (next == null) return null;
        int season = next.optInt("season");
        int number = next.optInt("number");
        OpenTarget target = StremioLink.target(targetPackage, StremioLink.episode(show.imdbId, season, number));
        return new ViewingItem(TraktPlayback.SOURCE_ID, "episode:" + TraktJson.id(next, "trakt"), show.showKey(),
                ViewingKind.NEXT, show.title, TraktJson.text(next, "title"), season, number, 0,
                next.optInt("runtime", 0) * 60_000L, show.lastWatchedMs, Posters.ref(Posters.TV, show.tmdbId),
                target);
    }
}
