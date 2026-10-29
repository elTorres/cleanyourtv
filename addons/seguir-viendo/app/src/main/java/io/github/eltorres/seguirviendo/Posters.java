package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Posters {
    static final String TV = "tv";
    static final String MOVIE = "movie";

    private final PosterStore store;
    private final ArtworkLookup lookup;

    Posters(PosterStore store, ArtworkLookup lookup) {
        this.store = store;
        this.lookup = lookup;
    }

    static String ref(String type, String tmdbId) {
        return tmdbId == null ? null : "tmdb:" + type + ":" + tmdbId;
    }

    boolean available() {
        return lookup != null;
    }

    List<ViewingItem> attach(List<ViewingItem> items) {
        List<ViewingItem> out = new ArrayList<>(items.size());
        for (ViewingItem item : items) {
            String uri = item.posterKey() == null ? null : uriFor(item.posterKey());
            out.add(uri == null ? item : item.withPosterUri(uri));
        }
        return out;
    }

    void retainOnly(Collection<ViewingItem> published) {
        Set<String> keys = new HashSet<>();
        for (ViewingItem item : published) {
            if (item.posterKey() != null) keys.add(item.posterKey());
        }
        store.retainOnly(keys);
    }

    private String uriFor(String key) {
        String stored = store.uriFor(key);
        if (stored != null || lookup == null) return stored;
        String[] parts = key.split(":", 3);
        if (parts.length != 3) return null;
        try {
            byte[] image = lookup.poster(parts[2], parts[1]);
            return image == null ? null : store.put(key, image);
        } catch (IOException e) {
            return null;
        }
    }
}
