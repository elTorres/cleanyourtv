package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.Map;

final class RoutedArtwork implements ArtworkLookup {
    private final Map<String, ArtworkLookup> byType;

    RoutedArtwork(Map<String, ArtworkLookup> byType) {
        this.byType = byType;
    }

    @Override
    public byte[] poster(String id, String type) throws IOException {
        ArtworkLookup lookup = byType.get(type);
        return lookup == null ? null : lookup.poster(id, type);
    }
}
