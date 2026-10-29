package io.github.eltorres.seguirviendo;

import java.io.IOException;

interface ArtworkLookup {
    byte[] poster(String tmdbId, String type) throws IOException;
}
