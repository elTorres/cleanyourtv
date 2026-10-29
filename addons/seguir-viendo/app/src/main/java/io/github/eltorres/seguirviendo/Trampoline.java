package io.github.eltorres.seguirviendo;

final class Trampoline {
    static final String STREMIO = "com.stremio.one";

    enum Action { OPEN_LINK, OPEN_APP, MISSING }

    private Trampoline() {
    }

    static String targetOrDefault(String target) {
        return target == null || target.isEmpty() ? STREMIO : target;
    }

    static Action choose(boolean linkResolves, boolean appLaunchable) {
        if (linkResolves) return Action.OPEN_LINK;
        return appLaunchable ? Action.OPEN_APP : Action.MISSING;
    }
}
