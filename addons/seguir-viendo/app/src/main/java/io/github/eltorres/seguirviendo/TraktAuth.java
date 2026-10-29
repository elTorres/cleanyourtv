package io.github.eltorres.seguirviendo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

final class TraktAuth {
    static final String API = "https://api.trakt.tv";
    static final String ACCESS_TOKEN = "trakt.access_token";
    static final String REFRESH_TOKEN = "trakt.refresh_token";
    static final String EXPIRES_AT = "trakt.expires_at";
    static final String RECONNECT_NEEDED = "trakt.reconnect_needed";
    private static final long REFRESH_MARGIN_MS = 24L * 60 * 60 * 1000;

    enum PollStatus { PENDING, SLOW_DOWN, CONNECTED, EXPIRED, DENIED, INVALID }

    static final class DeviceCode {
        final String deviceCode;
        final String userCode;
        final String verificationUrl;
        final long expiresAtMs;
        final long intervalMs;
        final String verifier;

        DeviceCode(String deviceCode, String userCode, String verificationUrl, long expiresAtMs, long intervalMs,
                   String verifier) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUrl = verificationUrl;
            this.expiresAtMs = expiresAtMs;
            this.intervalMs = intervalMs;
            this.verifier = verifier;
        }
    }

    static final class PollResult {
        final PollStatus status;

        PollResult(PollStatus status) {
            this.status = status;
        }
    }

    private final Http http;
    private final SecretStore secrets;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final LongSupplier clock;

    TraktAuth(Http http, SecretStore secrets, String clientId, String clientSecret, String redirectUri,
              LongSupplier clock) {
        this.http = http;
        this.secrets = secrets;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.clock = clock;
    }

    boolean available() {
        return clientId != null && !clientId.isEmpty();
    }

    SourceState state() {
        if (!available()) return SourceState.UNAVAILABLE;
        if (secrets.get(ACCESS_TOKEN) == null) return SourceState.DISCONNECTED;
        if (secrets.get(RECONNECT_NEEDED) != null) return SourceState.RECONNECT_NEEDED;
        return SourceState.CONNECTED;
    }

    String clientId() {
        return clientId;
    }

    String accessToken() {
        return secrets.get(ACCESS_TOKEN);
    }

    DeviceCode requestCode() throws IOException {
        String verifier = Pkce.newVerifier();
        JSONObject body = new JSONObject();
        put(body, "client_id", clientId);
        put(body, "code_challenge", Pkce.challenge(verifier));
        put(body, "code_challenge_method", Pkce.METHOD);
        Http.Response response = http.send("POST", API + "/oauth/device/code", jsonHeaders(), body.toString());
        if (!response.ok()) throw new IOException("Trakt device code: HTTP " + response.code);
        try {
            JSONObject json = new JSONObject(response.body);
            long now = clock.getAsLong();
            return new DeviceCode(json.getString("device_code"), json.getString("user_code"),
                    json.getString("verification_url"), now + json.getLong("expires_in") * 1000,
                    json.optLong("interval", 5) * 1000, verifier);
        } catch (JSONException e) {
            throw new IOException("Trakt device code: unreadable answer", e);
        }
    }

    PollResult poll(DeviceCode code) throws IOException {
        if (clock.getAsLong() >= code.expiresAtMs) return new PollResult(PollStatus.EXPIRED);
        JSONObject body = new JSONObject();
        put(body, "code", code.deviceCode);
        put(body, "client_id", clientId);
        put(body, "code_verifier", code.verifier);
        putSecret(body);
        Http.Response response = http.send("POST", API + "/oauth/device/token", jsonHeaders(), body.toString());
        switch (response.code) {
            case 200:
                saveTokens(response.body);
                return new PollResult(PollStatus.CONNECTED);
            case 400:
                return new PollResult(PollStatus.PENDING);
            case 429:
                return new PollResult(PollStatus.SLOW_DOWN);
            case 410:
                return new PollResult(PollStatus.EXPIRED);
            case 418:
                return new PollResult(PollStatus.DENIED);
            case 404:
            case 409:
                return new PollResult(PollStatus.INVALID);
            default:
                throw new IOException("Trakt device token: HTTP " + response.code);
        }
    }

    void refreshIfNeeded() throws IOException, AuthException {
        String refresh = secrets.get(REFRESH_TOKEN);
        if (refresh == null) throw rejected();
        long expiresAt = parseLong(secrets.get(EXPIRES_AT));
        if (expiresAt - clock.getAsLong() > REFRESH_MARGIN_MS) return;
        JSONObject body = new JSONObject();
        put(body, "refresh_token", refresh);
        put(body, "client_id", clientId);
        putSecret(body);
        put(body, "redirect_uri", redirectUri);
        put(body, "grant_type", "refresh_token");
        Http.Response response = http.send("POST", API + "/oauth/token", jsonHeaders(), body.toString());
        if (response.code == 400 || response.code == 401) throw rejected();
        if (!response.ok()) throw new IOException("Trakt token refresh: HTTP " + response.code);
        saveTokens(response.body);
    }

    AuthException rejected() {
        secrets.put(RECONNECT_NEEDED, "1");
        return new AuthException("Trakt rejected the session");
    }

    void disconnect() {
        secrets.remove(ACCESS_TOKEN);
        secrets.remove(REFRESH_TOKEN);
        secrets.remove(EXPIRES_AT);
        secrets.remove(RECONNECT_NEEDED);
    }

    Map<String, String> apiHeaders() {
        Map<String, String> headers = jsonHeaders();
        headers.put("trakt-api-version", "2");
        headers.put("trakt-api-key", clientId);
        headers.put("Authorization", "Bearer " + accessToken());
        return headers;
    }

    private void saveTokens(String body) throws IOException {
        try {
            JSONObject json = new JSONObject(body);
            long createdAtMs = json.optLong("created_at", clock.getAsLong() / 1000) * 1000;
            secrets.put(ACCESS_TOKEN, json.getString("access_token"));
            secrets.put(REFRESH_TOKEN, json.getString("refresh_token"));
            secrets.put(EXPIRES_AT, Long.toString(createdAtMs + json.getLong("expires_in") * 1000));
            secrets.remove(RECONNECT_NEEDED);
        } catch (JSONException e) {
            throw new IOException("Trakt token: unreadable answer", e);
        }
    }

    private void putSecret(JSONObject body) {
        if (clientSecret != null && !clientSecret.isEmpty()) put(body, "client_secret", clientSecret);
    }

    private static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static void put(JSONObject json, String key, String value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException(key, e);
        }
    }

    private static long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
