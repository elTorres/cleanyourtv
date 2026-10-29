package io.github.eltorres.seguirviendo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class FilePosterStore implements PosterStore {
    static final Pattern IMAGE_ID = Pattern.compile("[0-9a-f]{32}");

    private final File dir;
    private final String authority;

    FilePosterStore(File dir, String authority) {
        this.dir = dir;
        this.authority = authority;
    }

    static File file(File dir, String imageId) {
        return IMAGE_ID.matcher(imageId).matches() ? new File(dir, imageId) : null;
    }

    @Override
    public String uriFor(String posterKey) {
        String id = imageId(posterKey);
        return new File(dir, id).isFile() ? uri(id) : null;
    }

    @Override
    public String put(String posterKey, byte[] image) throws IOException {
        String id = imageId(posterKey);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        File partial = new File(dir, id + ".part");
        try (FileOutputStream out = new FileOutputStream(partial)) {
            out.write(image);
        }
        if (!partial.renameTo(new File(dir, id))) throw new IOException("cannot store poster " + id);
        return uri(id);
    }

    @Override
    public void retainOnly(Set<String> posterKeys) {
        Set<String> keep = new HashSet<>();
        for (String key : posterKeys) keep.add(imageId(key));
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!keep.contains(f.getName())) f.delete();
        }
    }

    private String uri(String id) {
        return "content://" + authority + "/" + id;
    }

    static String imageId(String posterKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(posterKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) hex.append(String.format("%02x", digest[i] & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
