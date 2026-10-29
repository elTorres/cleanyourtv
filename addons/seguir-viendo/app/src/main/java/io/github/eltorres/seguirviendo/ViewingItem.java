package io.github.eltorres.seguirviendo;

final class ViewingItem {
    private final String sourceId;
    private final String itemKey;
    private final String showKey;
    private final ViewingKind kind;
    private final String title;
    private final String episodeTitle;
    private final Integer season;
    private final Integer episode;
    private final long positionMs;
    private final long durationMs;
    private final long lastEngagedMs;
    private final String posterKey;
    private final OpenTarget target;
    private final String posterUri;

    ViewingItem(String sourceId, String itemKey, String showKey, ViewingKind kind, String title, String episodeTitle,
                Integer season, Integer episode, long positionMs, long durationMs, long lastEngagedMs,
                String posterKey, OpenTarget target) {
        this(sourceId, itemKey, showKey, kind, title, episodeTitle, season, episode, positionMs, durationMs,
                lastEngagedMs, posterKey, target, null);
    }

    private ViewingItem(String sourceId, String itemKey, String showKey, ViewingKind kind, String title,
                        String episodeTitle, Integer season, Integer episode, long positionMs, long durationMs,
                        long lastEngagedMs, String posterKey, OpenTarget target, String posterUri) {
        this.sourceId = sourceId;
        this.itemKey = itemKey;
        this.showKey = showKey;
        this.kind = kind;
        this.title = title;
        this.episodeTitle = episodeTitle;
        this.season = season;
        this.episode = episode;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.lastEngagedMs = lastEngagedMs;
        this.posterKey = posterKey;
        this.target = target;
        this.posterUri = posterUri;
    }

    ViewingItem withPosterUri(String uri) {
        return new ViewingItem(sourceId, itemKey, showKey, kind, title, episodeTitle, season, episode, positionMs,
                durationMs, lastEngagedMs, posterKey, target, uri);
    }

    String sourceId() { return sourceId; }

    String itemKey() { return itemKey; }

    String showKey() { return showKey; }

    ViewingKind kind() { return kind; }

    String title() { return title; }

    String episodeTitle() { return episodeTitle; }

    Integer season() { return season; }

    Integer episode() { return episode; }

    long positionMs() { return positionMs; }

    long durationMs() { return durationMs; }

    long lastEngagedMs() { return lastEngagedMs; }

    String posterKey() { return posterKey; }

    OpenTarget target() { return target; }

    String posterUri() { return posterUri; }

    boolean isEpisode() { return season != null && episode != null; }
}
