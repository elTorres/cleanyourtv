package io.github.eltorres.seguirviendo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MovistarViewings {
    static final String APP = "com.movistarplus.androidtv";
    static final String IMAGE = "image";
    static final String IMAGE_HOST = "https://estatico.emisiondof6.com/";
    static final long ORDER_STEP_MS = 60_000;
    private static final Pattern SEASON = Pattern.compile("\\(T(\\d+)\\)");
    private static final String[] WIDE_IMAGES = {"horizontal", "f169", "default"};

    private MovistarViewings() {
    }

    static List<ViewingItem> parse(String json, long nowMs) throws JSONException {
        JSONArray contents = new JSONObject(json).optJSONArray("Contenidos");
        List<ViewingItem> items = new ArrayList<>();
        for (int i = 0; contents != null && i < contents.length(); i++) {
            ViewingItem item = item(contents.getJSONObject(i), nowMs - items.size() * ORDER_STEP_MS);
            if (item != null) items.add(item);
        }
        return items;
    }

    static String link(String contentId) {
        return "appmovistar://movistarplus.es/ficha?id=" + contentId;
    }

    static String posterRef(String imageUri) {
        return imageUri == null || !imageUri.startsWith(IMAGE_HOST) ? null : MovistarAuth.SOURCE_ID + ":" + IMAGE + ":" + imageUri;
    }

    private static ViewingItem item(JSONObject content, long engagedMs) {
        JSONObject editorial = content.optJSONObject("DatosEditoriales");
        if (editorial == null) return null;
        String id = text(editorial, "Id");
        if (id == null) return null;
        JSONObject bookmark = content.optJSONObject("Bookmark");
        if (bookmark != null && bookmark.optBoolean("seen")) return null;
        long durationMs = seconds(bookmark, "duration", editorial.optLong("DuracionEnSegundos")) * 1000;
        long positionMs = seconds(bookmark, "offset", 0) * 1000;
        boolean episode = "Episodio".equals(editorial.optString("TipoContenido"));
        String showId = text(editorial, "IdSerie");
        String title = episode && text(editorial, "TituloSerie") != null
                ? text(editorial, "TituloSerie") : editorial.optString("Titulo");
        return new ViewingItem(MovistarAuth.SOURCE_ID, "content:" + id,
                showId == null ? "content:" + id : "show:" + showId,
                positionMs > 0 ? ViewingKind.CONTINUE : ViewingKind.NEXT, title,
                episode ? text(editorial, "TituloEpisodio") : null, episode ? season(editorial) : null,
                episode ? number(text(editorial, "NumeroEpisodio")) : null, positionMs, durationMs, engagedMs,
                posterRef(wideImage(editorial.optJSONArray("Imagenes"))), new OpenTarget(APP, link(id)));
    }

    private static Integer season(JSONObject editorial) {
        Matcher matcher = SEASON.matcher(editorial.optString("Titulo"));
        return matcher.find() ? number(matcher.group(1)) : null;
    }

    private static String wideImage(JSONArray images) {
        if (images == null) return null;
        for (String wanted : WIDE_IMAGES) {
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = images.optJSONObject(i);
                if (image != null && wanted.equals(image.optString("id"))) return text(image, "uri");
            }
        }
        return null;
    }

    private static long seconds(JSONObject json, String key, long fallback) {
        return json == null ? fallback : Math.max(0, json.optLong(key, fallback));
    }

    private static Integer number(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JSONObject json, String key) {
        if (json.isNull(key)) return null;
        String value = json.optString(key, "").trim();
        return value.isEmpty() ? null : value;
    }
}
