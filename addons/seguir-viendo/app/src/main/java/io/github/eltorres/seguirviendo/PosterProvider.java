package io.github.eltorres.seguirviendo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public final class PosterProvider extends ContentProvider {
    static final String AUTHORITY = WatchNextMapper.PACKAGE + ".posters";

    static File dir(android.content.Context context) {
        return new File(context.getCacheDir(), "posters");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new SecurityException("read only");
        List<String> segments = uri.getPathSegments();
        File file = segments.size() == 1 ? FilePosterStore.file(dir(getContext()), segments.get(0)) : null;
        if (file == null || !file.isFile()) throw new FileNotFoundException(uri.toString());
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new SecurityException("read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new SecurityException("read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new SecurityException("read only");
    }
}
