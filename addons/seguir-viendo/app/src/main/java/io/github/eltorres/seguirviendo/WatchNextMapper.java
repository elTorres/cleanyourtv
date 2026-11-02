package io.github.eltorres.seguirviendo;

import android.media.tv.TvContract.WatchNextPrograms;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class WatchNextMapper {
    static final String PACKAGE = "io.github.eltorres.seguirviendo";
    static final String TRAMPOLINE = PACKAGE + "/.TrampolineActivity";
    static final String EXTRA_TARGET = "target";
    static final String EXTRA_LINK = "link";
    private static final String DATA_SEPARATOR = "\n";

    private WatchNextMapper() {
    }

    static String providerId(ViewingItem item) {
        return item.sourceId() + ":" + item.itemKey();
    }

    static boolean belongsTo(String providerId, String sourceId) {
        return providerId != null && providerId.startsWith(sourceId + ":");
    }

    static Map<String, Object> columns(ViewingItem item, String posterUri) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(WatchNextPrograms.COLUMN_TYPE,
                item.isEpisode() ? WatchNextPrograms.TYPE_TV_EPISODE : WatchNextPrograms.TYPE_MOVIE);
        row.put(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, item.kind() == ViewingKind.CONTINUE
                ? WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE : WatchNextPrograms.WATCH_NEXT_TYPE_NEXT);
        row.put(WatchNextPrograms.COLUMN_TITLE, item.title());
        if (item.episodeTitle() != null) row.put(WatchNextPrograms.COLUMN_EPISODE_TITLE, item.episodeTitle());
        if (item.isEpisode()) {
            row.put(WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER, Integer.toString(item.season()));
            row.put(WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER, Integer.toString(item.episode()));
        }
        if (item.positionMs() > 0) row.put(WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, item.positionMs());
        if (item.durationMs() > 0) row.put(WatchNextPrograms.COLUMN_DURATION_MILLIS, item.durationMs());
        row.put(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, item.lastEngagedMs());
        row.put(WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, providerId(item));
        row.put(WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_DATA, providerData(item));
        row.put(WatchNextPrograms.COLUMN_SEARCHABLE, 1);
        row.put(WatchNextPrograms.COLUMN_INTENT_URI, intentUri(item.target()));
        if (posterUri != null) row.put(WatchNextPrograms.COLUMN_POSTER_ART_URI, posterUri);
        return row;
    }

    static byte[] providerData(ViewingItem item) {
        return String.join(DATA_SEPARATOR, item.showKey(), text(item.season()), text(item.episode()))
                .getBytes(StandardCharsets.UTF_8);
    }

    static PublishedRow published(long id, String providerId, byte[] providerData, String title) {
        if (providerId == null || providerData == null) return null;
        int split = providerId.indexOf(':');
        if (split <= 0) return null;
        String[] data = new String(providerData, StandardCharsets.UTF_8).split(DATA_SEPARATOR, -1);
        if (data.length != 3) return null;
        return new PublishedRow(id, providerId.substring(0, split), providerId.substring(split + 1), data[0], title,
                number(data[1]), number(data[2]));
    }

    private static String text(Integer value) {
        return value == null ? "" : Integer.toString(value);
    }

    private static Integer number(String value) {
        try {
            return value.isEmpty() ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String intentUri(OpenTarget target) {
        StringBuilder uri = new StringBuilder("intent:#Intent;component=").append(TRAMPOLINE);
        uri.append(";S.").append(EXTRA_TARGET).append('=').append(encode(target.targetPackage()));
        if (target.deepLink() != null) uri.append(";S.").append(EXTRA_LINK).append('=').append(encode(target.deepLink()));
        return uri.append(";end").toString();
    }

    private static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "-_.!~*'()".indexOf(c) >= 0) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return out.toString();
    }
}
