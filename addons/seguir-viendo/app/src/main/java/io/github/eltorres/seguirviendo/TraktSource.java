package io.github.eltorres.seguirviendo;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

final class TraktSource implements ViewingSource {
    private final TraktAuth auth;
    private final Http http;
    private final Supplier<String> targetPackage;

    TraktSource(TraktAuth auth, Http http, Supplier<String> targetPackage) {
        this.auth = auth;
        this.http = http;
        this.targetPackage = targetPackage;
    }

    @Override
    public String id() {
        return TraktPlayback.SOURCE_ID;
    }

    @Override
    public SourceState state() {
        return auth.state();
    }

    @Override
    public List<ViewingItem> fetch() throws IOException, AuthException {
        auth.refreshIfNeeded();
        Http.Response response = http.send("GET", TraktAuth.API + "/sync/playback?extended=full",
                auth.apiHeaders(), null);
        if (response.code == 401 || response.code == 403) throw auth.rejected();
        if (!response.ok()) throw new IOException("Trakt playback: HTTP " + response.code);
        try {
            return TraktPlayback.parse(response.body, new OpenTarget(targetPackage.get(), null));
        } catch (JSONException e) {
            throw new IOException("Trakt playback: unreadable answer", e);
        }
    }
}
