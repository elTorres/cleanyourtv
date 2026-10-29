package io.github.eltorres.seguirviendo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class UrlConnectionHttp implements Http {
    private static final int TIMEOUT_MS = 15_000;

    @Override
    public Response send(String method, String url, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Response(code, stream == null ? "" : read(stream));
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public byte[] download(String url, int maxBytes) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
            return bytes(connection.getInputStream(), maxBytes);
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws IOException {
        return new String(bytes(stream, Integer.MAX_VALUE), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(InputStream stream, int maxBytes) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) {
                if (out.size() + n > maxBytes) throw new IOException("larger than " + maxBytes + " bytes");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
}
