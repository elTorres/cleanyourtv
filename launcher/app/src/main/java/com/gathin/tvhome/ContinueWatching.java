package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The Continue watching row's data: the system's Watch Next table (docs/watch-next.md), read once per Home
 * resume off the main thread. Needs READ_TV_LISTINGS (build-and-install grants it); without it the query
 * throws and the row stays hidden.
 *
 * Only local posters are loaded (content://, android.resource://, file://). Most apps publish https://
 * poster URLs on their own CDNs; this app has no INTERNET permission, so those rows get a text card instead
 * and Home never makes a network request.
 */
final class ContinueWatching {
    static final String TAG = "TvHomeContinue";
    /** Rows older than this are dropped: some apps leave them for months (docs/watch-next.md, TCL C6K). */
    private static final long MAX_AGE_MS = 60L * 24 * 60 * 60 * 1000;
    /** A "continue" row this far through is finished in all but name. */
    private static final double DONE_FRACTION = 0.95;
    static final int MAX_ITEMS = 10;
    /** Poster decode width; a card is 160dp wide, 2x on this box. */
    private static final int POSTER_PX = 360;

    static final class Item {
        final String pkg;
        final String title;
        final String subtitle;
        final String intentUri;
        final float progress;      // 0..1, or -1 when the app gives no position
        final String posterUri;    // local posters only; null: the app's banner
        Bitmap poster;             // filled in by loadPoster, off the main thread

        Item(String pkg, String title, String subtitle, String intentUri, float progress, String posterUri) {
            this.pkg = pkg;
            this.title = title;
            this.subtitle = subtitle;
            this.intentUri = intentUri;
            this.progress = progress;
            this.posterUri = posterUri;
        }
    }

    /** One Watch Next row as read, before filtering; plain fields so the filter runs on the JVM in tests. */
    static final class ContinueEntry {
        final String pkg;
        final String title;
        final String subtitle;
        final String intentUri;
        final String posterUri;
        final boolean browsable;
        final long engagedMs;      // 0 when the app gives no engagement time
        final long positionMs;
        final long durationMs;
        final int type;            // TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_*, or -1

        ContinueEntry(String pkg, String title, String subtitle, String intentUri, String posterUri,
                      boolean browsable, long engagedMs, long positionMs, long durationMs, int type) {
            this.pkg = pkg;
            this.title = title;
            this.subtitle = subtitle;
            this.intentUri = intentUri;
            this.posterUri = posterUri;
            this.browsable = browsable;
            this.engagedMs = engagedMs;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.type = type;
        }

        float progress() {
            return positionMs > 0 && durationMs > 0 ? (float) Math.min(1.0, (double) positionMs / durationMs) : -1f;
        }
    }

    /** TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE, kept here so the filter needs no Android classes. */
    static final int TYPE_CONTINUE = 0;

    /** Launcher settings' Continue watching switch, kept with the other on-TV choices; a theme push leaves it. */
    static final String CHOICE_KEY = "continue_watching";

    private ContinueWatching() {
    }

    /** The on-TV choice when there is one, config.xml's continue_watching otherwise. */
    static boolean enabled(boolean configured, Boolean chosenOnTv) {
        return chosenOnTv != null ? chosenOnTv : configured;
    }

    static boolean enabled(Context context) {
        SharedPreferences choices = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        Boolean chosen = choices.contains(CHOICE_KEY) ? choices.getBoolean(CHOICE_KEY, false) : null;
        return enabled(context.getResources().getBoolean(R.bool.continue_watching), chosen);
    }

    /** Runs on a background thread. Never throws: any failure is one log line and an empty list. */
    static List<Item> load(Context context) {
        if (!enabled(context)) return new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        PackageManager pm = context.getPackageManager();
        Map<String, Integer> perPackage = new TreeMap<>();
        List<ContinueEntry> rows = new ArrayList<>();
        // No selection: a caller without ACCESS_ALL_EPG_DATA gets a SecurityException when it passes one.
        try (Cursor c = cr.query(TvContract.WatchNextPrograms.CONTENT_URI, null, null, null, null)) {
            if (c == null) {
                Log.w(TAG, "Watch Next query returned no cursor (no TV provider?)");
                return new ArrayList<>();
            }
            while (c.moveToNext()) {
                String pkg = str(c, TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME);
                if (pkg == null) continue;
                perPackage.merge(pkg, 1, Integer::sum);
                if (pm.getLeanbackLaunchIntentForPackage(pkg) == null && pm.getLaunchIntentForPackage(pkg) == null) continue;
                String poster = str(c, TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI);
                if (poster == null) poster = str(c, TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI);
                rows.add(new ContinueEntry(pkg, str(c, TvContract.WatchNextPrograms.COLUMN_TITLE), subtitle(c),
                        str(c, TvContract.WatchNextPrograms.COLUMN_INTENT_URI), poster,
                        lng(c, TvContract.WatchNextPrograms.COLUMN_BROWSABLE, 1) != 0,
                        lng(c, TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, 0),
                        lng(c, TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, 0),
                        lng(c, TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, 0),
                        (int) lng(c, TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, -1)));
            }
        } catch (Exception e) {
            // SecurityException without READ_TV_LISTINGS, or a provider that is missing or broken.
            Log.w(TAG, "Watch Next not readable: " + e);
            return new ArrayList<>();
        }
        Set<String> allowed = new HashSet<>(Arrays.asList(
                context.getResources().getStringArray(R.array.continue_watching_apps)));
        List<Item> items = new ArrayList<>();
        for (ContinueEntry row : filter(rows, System.currentTimeMillis(), allowed)) {
            Item item = new Item(row.pkg, row.title, row.subtitle, row.intentUri, row.progress(),
                    isLocal(row.posterUri) ? row.posterUri : null);
            item.poster = cached(item.posterUri);
            items.add(item);
        }
        Log.i(TAG, "Watch Next rows per app: " + perPackage + ", shown: " + items.size());
        return items;
    }

    /** The rows worth a card: from an allowed app (every app when none is listed), browsable, titled, engaged in
     *  the last 60 days, not finished; newest first, the newest row of each show only, at most MAX_ITEMS. */
    static List<ContinueEntry> filter(List<ContinueEntry> rows, long nowMs, Set<String> allowedApps) {
        List<ContinueEntry> kept = new ArrayList<>();
        for (ContinueEntry row : rows) {
            if (!allowedApps.isEmpty() && !allowedApps.contains(row.pkg)) continue;
            if (!row.browsable || row.title == null || row.title.isEmpty()) continue;
            if (row.engagedMs > 0 && nowMs - row.engagedMs > MAX_AGE_MS) continue;
            if (row.type == TYPE_CONTINUE && row.progress() >= DONE_FRACTION) continue;
            kept.add(row);
        }
        Collections.sort(kept, (a, b) -> Long.compare(b.engagedMs, a.engagedMs));
        Set<String> seen = new HashSet<>();
        List<ContinueEntry> shown = new ArrayList<>();
        for (ContinueEntry row : kept) {
            if (shown.size() == MAX_ITEMS) break;
            if (seen.add(row.pkg + '\n' + row.title)) shown.add(row);
        }
        return shown;
    }

    /** "S2 · E5 · Episode title", or whatever part of it the app filled in. */
    private static String subtitle(Cursor c) {
        StringBuilder s = new StringBuilder();
        String season = str(c, TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER);
        String episode = str(c, TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER);
        String name = str(c, TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE);
        if (season != null && !season.isEmpty()) s.append('S').append(season);
        if (episode != null && !episode.isEmpty()) s.append(s.length() > 0 ? " · " : "").append('E').append(episode);
        if (name != null && !name.isEmpty()) s.append(s.length() > 0 ? " · " : "").append(name);
        return s.toString();
    }

    /** Decoded posters by URI, so Home coming back does not fetch them again: Jellyfin's provider asks its
     *  server for each one, about four seconds a poster on this network. A dozen 360px RGB_565 posters is
     *  about 3 MB. */
    private static final Map<String, Bitmap> CACHE = new LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
            return size() > MAX_ITEMS + 2;
        }
    };

    private static boolean isLocal(String uri) {
        if (uri == null) return false;
        String scheme = Uri.parse(uri).getScheme();
        return "content".equals(scheme) || "android.resource".equals(scheme) || "file".equals(scheme);
    }

    private static Bitmap cached(String uri) {
        if (uri == null) return null;
        synchronized (CACHE) {
            return CACHE.get(uri);
        }
    }

    /** Off the main thread, one at a time: fills item.poster and returns true when there is one to show. */
    static boolean loadPoster(Context context, Item item) {
        if (item.posterUri == null || item.poster != null) return false;
        Bitmap b = decode(context.getContentResolver(), item.posterUri);
        if (b == null) return false;
        synchronized (CACHE) {
            CACHE.put(item.posterUri, b);
        }
        item.poster = b;
        return true;
    }

    private static Bitmap decode(ContentResolver cr, String uri) {
        Uri u = Uri.parse(uri);
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(u)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (bounds.outWidth / (opts.inSampleSize * 2) >= POSTER_PX) opts.inSampleSize *= 2;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;   // half the memory, posters have no alpha
            try (InputStream in = cr.openInputStream(u)) {
                return in == null ? null : BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "poster not loaded: " + e);
            return null;
        }
    }

    /** The intent the app published, or its own launch intent when that is missing or unparseable. */
    static Intent intentFor(Context context, Item item) {
        if (item.intentUri != null) {
            try {
                Intent i = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME);
                if (i.getPackage() == null && i.getComponent() == null) i.setPackage(item.pkg);
                String target = i.getComponent() != null ? i.getComponent().getPackageName() : i.getPackage();
                if (publisherOwns(item.pkg, target)) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return i;
                }
            } catch (URISyntaxException e) {
                Log.w(TAG, "bad intent_uri from " + item.pkg + ": " + e);
            }
        }
        PackageManager pm = context.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(item.pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(item.pkg);
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /** Only the app that published the row: an intent_uri pointing elsewhere is not followed. */
    static boolean publisherOwns(String publisher, String target) {
        return publisher != null && publisher.equals(target);
    }

    private static String str(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        return i < 0 || c.isNull(i) ? null : c.getString(i);
    }

    private static long lng(Cursor c, String column, long fallback) {
        int i = c.getColumnIndex(column);
        return i < 0 || c.isNull(i) ? fallback : c.getLong(i);
    }
}
