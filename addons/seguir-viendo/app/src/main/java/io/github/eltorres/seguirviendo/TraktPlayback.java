package io.github.eltorres.seguirviendo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

final class TraktPlayback {
    static final String SOURCE_ID = "trakt";
    static final long UNKNOWN_RUNTIME_MS = 100L * 60 * 1000;

    private TraktPlayback() {
    }

    static List<ViewingItem> parse(String json, OpenTarget target) throws JSONException {
        JSONArray entries = new JSONArray(json);
        List<ViewingItem> items = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            ViewingItem item = item(entries.getJSONObject(i), target);
            if (item != null) items.add(item);
        }
        return items;
    }

    private static ViewingItem item(JSONObject entry, OpenTarget target) {
        String type = entry.optString("type");
        double progress = Math.max(0, Math.min(100, entry.optDouble("progress", 0)));
        long engaged = instant(entry.optString("paused_at", null));
        if ("episode".equals(type)) {
            JSONObject episode = entry.optJSONObject("episode");
            JSONObject show = entry.optJSONObject("show");
            if (episode == null || show == null) return null;
            long duration = runtime(episode);
            return new ViewingItem(SOURCE_ID, "episode:" + traktId(episode), "show:" + traktId(show),
                    ViewingKind.CONTINUE, show.optString("title"), emptyToNull(episode.optString("title")),
                    episode.optInt("season"), episode.optInt("number"), Math.round(duration * progress / 100),
                    duration, engaged, null, target);
        }
        if ("movie".equals(type)) {
            JSONObject movie = entry.optJSONObject("movie");
            if (movie == null) return null;
            long duration = runtime(movie);
            String key = "movie:" + traktId(movie);
            return new ViewingItem(SOURCE_ID, key, key, ViewingKind.CONTINUE, movie.optString("title"), null,
                    null, null, Math.round(duration * progress / 100), duration, engaged, null, target);
        }
        return null;
    }

    private static long runtime(JSONObject media) {
        int minutes = media.optInt("runtime", 0);
        return minutes > 0 ? minutes * 60_000L : UNKNOWN_RUNTIME_MS;
    }

    private static String traktId(JSONObject media) {
        JSONObject ids = media.optJSONObject("ids");
        return ids == null ? "" : ids.optString("trakt");
    }

    private static long instant(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
