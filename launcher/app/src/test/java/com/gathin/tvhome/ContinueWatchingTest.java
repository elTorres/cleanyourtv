package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Which Watch Next rows become cards in the Continue watching row, and which intents it follows. */
public class ContinueWatchingTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final int TYPE_NEXT = 1;
    private static final Set<String> ALL = Set.of();

    private static ContinueWatching.ContinueEntry row(String pkg, String title, long ageMs, int type,
                                                      long positionMs, long durationMs) {
        return new ContinueWatching.ContinueEntry(pkg, title, "", null, null, true, NOW - ageMs,
                positionMs, durationMs, type);
    }

    private static ContinueWatching.ContinueEntry row(String title, long ageMs) {
        return row("com.stremio.one", title, ageMs, TYPE_NEXT, 0, 0);
    }

    private static List<String> titles(List<ContinueWatching.ContinueEntry> rows) {
        List<String> out = new ArrayList<>();
        for (ContinueWatching.ContinueEntry r : rows) out.add(r.title);
        return out;
    }

    @Test
    public void dropsRowsOlderThanSixtyDays() {
        List<ContinueWatching.ContinueEntry> shown = ContinueWatching.filter(
                List.of(row("Fresh", 2 * DAY), row("Stale", 90 * DAY)), NOW, ALL);
        assertEquals(List.of("Fresh"), titles(shown));
    }

    @Test
    public void dropsContinueRowsPastNinetyFivePercent() {
        List<ContinueWatching.ContinueEntry> shown = ContinueWatching.filter(List.of(
                row("com.stremio.one", "Almost done", DAY, ContinueWatching.TYPE_CONTINUE, 99, 100),
                row("com.stremio.one", "Halfway", DAY, ContinueWatching.TYPE_CONTINUE, 50, 100)), NOW, ALL);
        assertEquals(List.of("Halfway"), titles(shown));
    }

    @Test
    public void keepsNewestRowPerShow() {
        ContinueWatching.ContinueEntry older = new ContinueWatching.ContinueEntry("com.stremio.one", "The Bear",
                "S3 · E1", null, null, true, NOW - 5 * DAY, 0, 0, TYPE_NEXT);
        ContinueWatching.ContinueEntry newer = new ContinueWatching.ContinueEntry("com.stremio.one", "The Bear",
                "S3 · E2", null, null, true, NOW - DAY, 0, 0, TYPE_NEXT);
        List<ContinueWatching.ContinueEntry> shown = ContinueWatching.filter(List.of(older, newer), NOW, ALL);
        assertEquals(1, shown.size());
        assertEquals("S3 · E2", shown.get(0).subtitle);
    }

    @Test
    public void capsAtTenNewestFirst() {
        List<ContinueWatching.ContinueEntry> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) rows.add(row("Show " + i, i * DAY));
        Collections.shuffle(rows);
        List<String> shown = titles(ContinueWatching.filter(rows, NOW, ALL));
        assertEquals(ContinueWatching.MAX_ITEMS, shown.size());
        assertEquals("Show 0", shown.get(0));
        assertEquals("Show 9", shown.get(9));
    }

    @Test
    public void allowedAppsOnlyWhenListed() {
        List<ContinueWatching.ContinueEntry> rows = List.of(
                row("com.example.publisher", "Paused episode", 3 * DAY, ContinueWatching.TYPE_CONTINUE, 40, 100),
                row("com.example.firmware", "Promotion", 0, TYPE_NEXT, 0, 0));
        assertEquals(List.of("Paused episode"),
                titles(ContinueWatching.filter(rows, NOW, Set.of("com.example.publisher"))));
    }

    @Test
    public void emptyAllowListShowsEveryApp() {
        List<ContinueWatching.ContinueEntry> rows = List.of(
                row("com.example.publisher", "Paused episode", 3 * DAY, ContinueWatching.TYPE_CONTINUE, 40, 100),
                row("com.example.firmware", "Promotion", 0, TYPE_NEXT, 0, 0));
        assertEquals(List.of("Promotion", "Paused episode"), titles(ContinueWatching.filter(rows, NOW, ALL)));
    }

    @Test
    public void emptyInputHidesRow() {
        assertTrue(ContinueWatching.filter(List.of(), NOW, ALL).isEmpty());
        ContinueWatching.ContinueEntry hidden = new ContinueWatching.ContinueEntry("com.stremio.one", "Hidden",
                "", null, null, false, NOW, 0, 0, TYPE_NEXT);
        assertTrue(ContinueWatching.filter(List.of(hidden, row("", DAY)), NOW, ALL).isEmpty());
    }

    @Test
    public void followsIntentOnlyFromPublisher() {
        assertTrue(ContinueWatching.publisherOwns("com.example.publisher", "com.example.publisher"));
        assertFalse(ContinueWatching.publisherOwns("com.example.publisher", "com.stremio.one"));
        assertFalse(ContinueWatching.publisherOwns("com.example.publisher", null));
    }
}
