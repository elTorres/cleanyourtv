package io.github.eltorres.reconectarmandos;

final class Attempt {
    private static final String SEPARATOR = "|";

    private final long atMs;
    private final boolean connected;
    private final String detail;

    Attempt(long atMs, boolean connected, String detail) {
        this.atMs = atMs;
        this.connected = connected;
        this.detail = detail == null ? "" : detail.replace('\n', ' ');
    }

    long atMs() {
        return atMs;
    }

    boolean connected() {
        return connected;
    }

    String detail() {
        return detail;
    }

    String encode() {
        return atMs + SEPARATOR + (connected ? 1 : 0) + SEPARATOR + detail;
    }

    static Attempt decode(String stored) {
        if (stored == null) return null;
        String[] parts = stored.split("\\" + SEPARATOR, 3);
        if (parts.length < 3) return null;
        try {
            return new Attempt(Long.parseLong(parts[0]), "1".equals(parts[1]), parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
