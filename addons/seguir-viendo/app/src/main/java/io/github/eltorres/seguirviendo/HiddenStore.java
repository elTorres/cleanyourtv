package io.github.eltorres.seguirviendo;

import java.util.Set;

interface HiddenStore {
    Set<String> keys();

    void add(String key);
}
