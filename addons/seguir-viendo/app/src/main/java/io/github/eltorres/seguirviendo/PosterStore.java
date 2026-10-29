package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.Set;

interface PosterStore {
    String uriFor(String posterKey);

    String put(String posterKey, byte[] image) throws IOException;

    void retainOnly(Set<String> posterKeys);
}
