package io.github.eltorres.seguirviendo;

import java.util.List;
import java.util.function.Predicate;

final class Trampoline {
    static final String STREMIO = "com.stremio.one";
    static final String KODI = "org.xbmc.kodi";
    static final List<String> TARGETS = List.of(STREMIO, KODI);

    enum Action { OPEN_LINK, OPEN_APP, MISSING }

    private Trampoline() {
    }

    static String targetOrDefault(String target) {
        return target == null || target.isEmpty() ? STREMIO : target;
    }

    static String nextTarget(String current, Predicate<String> installed) {
        int start = TARGETS.indexOf(current);
        for (int step = 1; step <= TARGETS.size(); step++) {
            String candidate = TARGETS.get(Math.floorMod(start + step, TARGETS.size()));
            if (installed.test(candidate)) return candidate;
        }
        return targetOrDefault(current);
    }

    static Action choose(boolean linkResolves, boolean appLaunchable) {
        if (linkResolves) return Action.OPEN_LINK;
        return appLaunchable ? Action.OPEN_APP : Action.MISSING;
    }
}
