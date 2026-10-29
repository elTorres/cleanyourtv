package io.github.eltorres.seguirviendo;

final class StremioLink {
    private StremioLink() {
    }

    static String episode(String showImdbId, int season, int episode) {
        if (!isImdb(showImdbId)) return null;
        return "stremio:///detail/series/" + showImdbId + "/" + showImdbId + ":" + season + ":" + episode;
    }

    static String movie(String imdbId) {
        return isImdb(imdbId) ? "stremio:///detail/movie/" + imdbId : null;
    }

    static OpenTarget target(String targetPackage, String link) {
        return new OpenTarget(targetPackage, Trampoline.STREMIO.equals(targetPackage) ? link : null);
    }

    private static boolean isImdb(String id) {
        return id != null && id.matches("tt\\d+");
    }
}
