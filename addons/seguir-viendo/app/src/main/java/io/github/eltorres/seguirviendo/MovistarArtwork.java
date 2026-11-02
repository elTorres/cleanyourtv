package io.github.eltorres.seguirviendo;

import java.io.IOException;

final class MovistarArtwork implements ArtworkLookup {
    private static final int MAX_IMAGE_BYTES = 512 * 1024;

    private final Http http;

    MovistarArtwork(Http http) {
        this.http = http;
    }

    @Override
    public byte[] poster(String imageUri, String type) throws IOException {
        if (imageUri == null || !imageUri.startsWith(MovistarViewings.IMAGE_HOST)) return null;
        return http.download(imageUri, MAX_IMAGE_BYTES);
    }
}
