package io.github.eltorres.seguirviendo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class TraktPlayback {
    static final String SOURCE_ID = "trakt";
    static final long UNKNOWN_RUNTIME_MS = 100L * 60 * 1000;

    private TraktPlayback() {
    }

    static List<ViewingItem> parse(String json, String targetPackage) throws JSONException {
        JSONArray entries = new JSONArray(json);
        List<ViewingItem> items = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            ViewingItem item = item(entries.getJSONObject(i), targetPackage);
            if (item != null) items.add(item);
        }
        return items;
    }

    private static ViewingItem item(JSONObject entry, String targetPackage) {
        String type = entry.optString("type");
        double progress = Math.max(0, Math.min(100, entry.optDouble("progress", 0)));
        long engaged = TraktJson.instant(entry.optString("paused_at", null));
        if ("episode".equals(type)) {
            JSONObject episode = entry.optJSONObject("episode");
            JSONObject show = entry.optJSONObject("show");
            if (episode == null || show == null) return null;
            long duration = runtime(episode);
            int season = episode.optInt("season");
            int number = episode.optInt("number");
            OpenTarget target = StremioLink.target(targetPackage,
                    StremioLink.episode(TraktJson.id(show, "imdb"), season, number));
            return new ViewingItem(SOURCE_ID, "episode:" + TraktJson.id(episode, "trakt"),
                    "show:" + TraktJson.id(show, "trakt"), ViewingKind.CONTINUE, show.optString("title"),
                    TraktJson.text(episode, "title"), season, number, Math.round(duration * progress / 100),
                    duration, engaged, Posters.ref(Posters.TV, TraktJson.id(show, "tmdb")), target);
        }
        if ("movie".equals(type)) {
            JSONObject movie = entry.optJSONObject("movie");
            if (movie == null) return null;
            long duration = runtime(movie);
            String key = "movie:" + TraktJson.id(movie, "trakt");
            OpenTarget target = StremioLink.target(targetPackage, StremioLink.movie(TraktJson.id(movie, "imdb")));
            return new ViewingItem(SOURCE_ID, key, key, ViewingKind.CONTINUE, movie.optString("title"), null,
                    null, null, Math.round(duration * progress / 100), duration, engaged,
                    Posters.ref(Posters.MOVIE, TraktJson.id(movie, "tmdb")), target);
        }
        return null;
    }

    private static long runtime(JSONObject media) {
        int minutes = media.optInt("runtime", 0);
        return minutes > 0 ? minutes * 60_000L : UNKNOWN_RUNTIME_MS;
    }
}
