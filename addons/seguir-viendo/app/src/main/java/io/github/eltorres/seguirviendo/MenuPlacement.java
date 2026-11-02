package io.github.eltorres.seguirviendo;

final class MenuPlacement {
    private final int x;
    private final int y;
    private final boolean above;

    private MenuPlacement(int x, int y, boolean above) {
        this.x = x;
        this.y = y;
        this.above = above;
    }

    static MenuPlacement centered(int menuWidth, int menuHeight, int screenWidth, int screenHeight) {
        return new MenuPlacement((screenWidth - menuWidth) / 2, (screenHeight - menuHeight) / 2, false);
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    boolean above() {
        return above;
    }
}
