package io.github.eltorres.seguirviendo;

import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;

final class TraktJson {
    private TraktJson() {
    }

    static String id(JSONObject media, String kind) {
        JSONObject ids = media == null ? null : media.optJSONObject("ids");
        if (ids == null || ids.isNull(kind)) return null;
        String value = ids.optString(kind);
        return value.isEmpty() ? null : value;
    }

    static long instant(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    static String text(JSONObject object, String key) {
        if (object == null || object.isNull(key)) return null;
        String value = object.optString(key);
        return value.isEmpty() ? null : value;
    }
}
