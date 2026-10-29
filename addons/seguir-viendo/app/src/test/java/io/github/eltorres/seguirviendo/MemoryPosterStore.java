package io.github.eltorres.seguirviendo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class MemoryPosterStore implements PosterStore {
    final Map<String, byte[]> images = new LinkedHashMap<>();

    @Override
    public String uriFor(String posterKey) {
        return images.containsKey(posterKey) ? "content://posters/" + posterKey : null;
    }

    @Override
    public String put(String posterKey, byte[] image) {
        images.put(posterKey, image);
        return uriFor(posterKey);
    }

    @Override
    public void retainOnly(Set<String> posterKeys) {
        images.keySet().retainAll(posterKeys);
    }
}
