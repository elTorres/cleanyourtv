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

    static MenuPlacement nextTo(int cardLeft, int cardTop, int cardRight, int cardBottom, int menuWidth, int menuHeight,
                                int screenWidth, int screenHeight, int gap, boolean rtl) {
        int fromStart = rtl ? cardRight - menuWidth : cardLeft;
        int fromEnd = rtl ? cardLeft : cardRight - menuWidth;
        boolean startFits = fromStart >= 0 && fromStart + menuWidth <= screenWidth;
        int x = Math.max(0, Math.min(startFits ? fromStart : fromEnd, screenWidth - menuWidth));
        if (cardBottom + gap + menuHeight <= screenHeight) return new MenuPlacement(x, cardBottom + gap, false);
        return new MenuPlacement(x, Math.max(0, cardTop - gap - menuHeight), true);
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
