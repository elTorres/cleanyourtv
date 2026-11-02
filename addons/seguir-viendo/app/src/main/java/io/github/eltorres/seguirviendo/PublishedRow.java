package io.github.eltorres.seguirviendo;

final class PublishedRow {
    private final long id;
    private final String sourceId;
    private final String itemKey;
    private final String showKey;
    private final String title;
    private final Integer season;
    private final Integer episode;

    PublishedRow(long id, String sourceId, String itemKey, String showKey, String title, Integer season,
                 Integer episode) {
        this.id = id;
        this.sourceId = sourceId;
        this.itemKey = itemKey;
        this.showKey = showKey;
        this.title = title;
        this.season = season;
        this.episode = episode;
    }

    long id() { return id; }

    String sourceId() { return sourceId; }

    String itemKey() { return itemKey; }

    String showKey() { return showKey; }

    String title() { return title; }

    Integer season() { return season; }

    Integer episode() { return episode; }
}
