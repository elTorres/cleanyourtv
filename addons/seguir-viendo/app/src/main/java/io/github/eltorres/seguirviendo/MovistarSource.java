package io.github.eltorres.seguirviendo;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

final class MovistarSource implements ViewingSource {
    private static final String VIEWINGS = "https://perso.dof6.com/movistarplus/" + MovistarAuth.DEVICE_CLASS + "/users/";
    private static final int PAGE = 20;
    static final int MAX_ITEMS = 3;

    private final MovistarAuth auth;
    private final Http http;
    private final LongSupplier clock;

    MovistarSource(MovistarAuth auth, Http http, LongSupplier clock) {
        this.auth = auth;
        this.http = http;
        this.clock = clock;
    }

    @Override
    public String id() {
        return MovistarAuth.SOURCE_ID;
    }

    @Override
    public SourceState state() {
        return auth.state();
    }

    @Override
    public List<ViewingItem> fetch() throws IOException, AuthException {
        MovistarAuth.Session session = auth.openSession();
        Map<String, String> headers = auth.bearer();
        headers.put("X-Hzid", session.token);
        headers.put("Accept", "application/vnd.miviewtv.v1+json");
        Http.Response response = http.send("GET", url(session), headers, null);
        if (response.code == 401 || response.code == 403) throw auth.expired("viewings");
        if (!response.ok()) throw new IOException("Movistar Plus viewings: HTTP " + response.code);
        try {
            List<ViewingItem> items = MovistarViewings.parse(response.body, clock.getAsLong());
            return items.size() > MAX_ITEMS ? new ArrayList<>(items.subList(0, MAX_ITEMS)) : items;
        } catch (JSONException e) {
            throw new IOException("Movistar Plus viewings: unreadable answer", e);
        }
    }

    String url(MovistarAuth.Session session) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("profile", auth.platform());
        query.put("container", "trackedseries");
        query.put("mediaType", "FOTOH");
        query.put("idsOnly", "false");
        query.put("accountNumber", auth.account());
        query.put("version", "8");
        query.put("start", "1");
        query.put("end", Integer.toString(PAGE));
        query.put("mdrm", "true");
        query.put("tlsstream", "true");
        query.put("demarcation", session.demarcation);
        query.put("filter", "AD-SINX");
        StringBuilder url = new StringBuilder(VIEWINGS).append(encode(auth.user())).append("/viewings");
        char separator = '?';
        for (Map.Entry<String, String> entry : query.entrySet()) {
            url.append(separator).append(entry.getKey()).append('=').append(encode(entry.getValue()));
            separator = '&';
        }
        return url.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
