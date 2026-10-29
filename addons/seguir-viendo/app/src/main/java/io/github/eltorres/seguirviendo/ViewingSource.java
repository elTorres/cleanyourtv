package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.List;

interface ViewingSource {
    String id();

    SourceState state();

    List<ViewingItem> fetch() throws IOException, AuthException;
}
