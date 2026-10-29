package io.github.eltorres.seguirviendo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

final class TmdbArtwork implements ArtworkLookup {
    static final String API = "https://api.themoviedb.org/3/";
    static final String IMAGES = "https://image.tmdb.org/t/p/w300";
    private static final int MAX_IMAGE_BYTES = 512 * 1024;

    private final Http http;
    private final String apiKey;
    private final String language;

    TmdbArtwork(Http http, String apiKey, String language) {
        this.http = http;
        this.apiKey = apiKey;
        this.language = language;
    }

    static ArtworkLookup orNull(Http http, String apiKey, String language) {
        return apiKey == null || apiKey.isEmpty() ? null : new TmdbArtwork(http, apiKey, language);
    }

    @Override
    public byte[] poster(String tmdbId, String type) throws IOException {
        if (!tmdbId.matches("\\d+") || !(Posters.TV.equals(type) || Posters.MOVIE.equals(type))) return null;
        boolean readToken = apiKey.startsWith("eyJ");
        String url = API + type + "/" + tmdbId + "?language=" + language + (readToken ? "" : "&api_key=" + apiKey);
        Map<String, String> headers = readToken
                ? Map.of("Authorization", "Bearer " + apiKey, "Accept", "application/json")
                : Map.of("Accept", "application/json");
        Http.Response response = http.send("GET", url, headers, null);
        if (response.code == 404) return null;
        if (!response.ok()) throw new IOException("TMDB " + type + ": HTTP " + response.code);
        String path;
        try {
            JSONObject details = new JSONObject(response.body);
            path = TraktJson.text(details, "backdrop_path");
            if (path == null) path = TraktJson.text(details, "poster_path");
        } catch (JSONException e) {
            throw new IOException("TMDB " + type + ": unreadable answer", e);
        }
        return path == null ? null : http.download(IMAGES + path, MAX_IMAGE_BYTES);
    }
}
