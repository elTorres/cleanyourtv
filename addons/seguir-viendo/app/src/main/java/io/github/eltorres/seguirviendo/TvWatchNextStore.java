package io.github.eltorres.seguirviendo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.WatchNextPrograms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TvWatchNextStore implements WatchNextStore {
    private final ContentResolver resolver;

    TvWatchNextStore(ContentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void replaceSource(String sourceId, List<ViewingItem> items) {
        clearSource(sourceId);
        for (ViewingItem item : items) resolver.insert(WatchNextPrograms.CONTENT_URI, values(item));
    }

    @Override
    public void clearSource(String sourceId) {
        for (long id : ownRows(sourceId)) resolver.delete(TvContract.buildWatchNextProgramUri(id), null, null);
    }

    private List<Long> ownRows(String sourceId) {
        List<Long> ids = new ArrayList<>();
        String[] projection = {WatchNextPrograms._ID, WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                WatchNextPrograms.COLUMN_PACKAGE_NAME};
        try (Cursor cursor = resolver.query(WatchNextPrograms.CONTENT_URI, projection, null, null, null)) {
            if (cursor == null) return ids;
            while (cursor.moveToNext()) {
                boolean ours = WatchNextMapper.PACKAGE.equals(cursor.getString(2));
                if (ours && WatchNextMapper.belongsTo(cursor.getString(1), sourceId)) ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    private static ContentValues values(ViewingItem item) {
        ContentValues values = new ContentValues();
        for (Map.Entry<String, Object> column : WatchNextMapper.columns(item, item.posterUri()).entrySet()) {
            Object value = column.getValue();
            if (value instanceof Integer) values.put(column.getKey(), (Integer) value);
            else if (value instanceof Long) values.put(column.getKey(), (Long) value);
            else values.put(column.getKey(), String.valueOf(value));
        }
        return values;
    }
}
