package io.github.eltorres.seguirviendo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

final class MovistarAuth {
    static final String SOURCE_ID = "movistar";
    static final String DEVICE_CLASS = "amazon.tv";
    static final String USERNAME = "movistar.username";
    static final String PASSWORD = "movistar.password";
    static final String ACCESS_TOKEN = "movistar.access_token";
    static final String EXPIRES_AT = "movistar.expires_at";
    static final String ACCOUNT = "movistar.account";
    static final String PLATFORM = "movistar.platform";
    static final String USER = "movistar.user";
    static final String DEVICE_ID = "movistar.device_id";
    static final String PROFILE_ID = "movistar.profile_id";
    static final String RECONNECT_NEEDED = "movistar.reconnect_needed";
    static final String MAIN_PROFILE = "0";

    private static final String AUTH = "https://auth.dof6.com";
    private static final String CLIENT_SERVICES = "https://clientservices.dof6.com/movistarplus";
    private static final String PROFILES = "https://grmovistar.imagenio.telefonica.net/asfe/rest/users/profiles?state=0&isForKids=0";
    private static final long RENEW_MARGIN_MS = 24L * 60 * 60 * 1000;
    private static final String[] KEYS = {USERNAME, PASSWORD, ACCESS_TOKEN, EXPIRES_AT, ACCOUNT, PLATFORM, USER,
            DEVICE_ID, PROFILE_ID, RECONNECT_NEEDED};

    enum LoginResult { CONNECTED, BAD_PASSWORD }

    static final class Device {
        final String id;
        final String name;
        final String type;

        Device(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    static final class Profile {
        final String id;
        final String name;

        Profile(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static final class Session {
        final String token;
        final String demarcation;

        Session(String token, String demarcation) {
            this.token = token;
            this.demarcation = demarcation;
        }
    }

    private final Http http;
    private final SecretStore secrets;
    private final LongSupplier clock;

    MovistarAuth(Http http, SecretStore secrets, LongSupplier clock) {
        this.http = http;
        this.secrets = secrets;
        this.clock = clock;
    }

    SourceState state() {
        if (secrets.get(ACCESS_TOKEN) == null || secrets.get(DEVICE_ID) == null) return SourceState.DISCONNECTED;
        if (secrets.get(RECONNECT_NEEDED) != null) return SourceState.RECONNECT_NEEDED;
        return SourceState.CONNECTED;
    }

    LoginResult login(String user, String password) throws IOException {
        String token = requestToken(user, password);
        if (token == null) return LoginResult.BAD_PASSWORD;
        JSONObject account = accountInfo(token);
        try {
            JSONObject offer = account.getJSONArray("ofertas").getJSONObject(0);
            secrets.put(ACCOUNT, offer.getString("accountNumber"));
            secrets.put(PLATFORM, offer.getString("@id_perfil"));
            secrets.put(USER, account.getString("cod_usuario_cifrado"));
        } catch (JSONException e) {
            throw new IOException("Movistar Plus account: unreadable answer", e);
        }
        secrets.put(USERNAME, user);
        secrets.put(PASSWORD, password);
        secrets.remove(RECONNECT_NEEDED);
        return LoginResult.CONNECTED;
    }

    List<Device> devices() throws IOException, AuthException {
        refreshIfNeeded();
        Http.Response response = http.send("GET", CLIENT_SERVICES + "/accounts/" + encode(secrets.get(ACCOUNT))
                + "/devices?qspVersion=ssp", bearer(), null);
        check(response, "devices");
        List<Device> devices = new ArrayList<>();
        try {
            JSONArray list = new JSONArray(response.body);
            for (int i = 0; i < list.length(); i++) {
                JSONObject device = list.getJSONObject(i);
                String id = device.optString("Id");
                if (id.isEmpty() || "-".equals(id)) continue;
                devices.add(new Device(id, device.optString("Name"), device.optString("DeviceType")));
            }
        } catch (JSONException e) {
            throw new IOException("Movistar Plus devices: unreadable answer", e);
        }
        return devices;
    }

    void selectDevice(String deviceId) {
        secrets.put(DEVICE_ID, deviceId);
    }

    void registerDevice() throws IOException, AuthException {
        refreshIfNeeded();
        String base = AUTH + "/movistarplus/" + DEVICE_CLASS + "/accounts/" + encode(secrets.get(ACCOUNT)) + "/devices/";
        Http.Response created = http.send("POST", base + "?qspVersion=ssp", jsonHeaders(true), "");
        check(created, "new device");
        String deviceId = created.body.trim().replace("\"", "");
        if (deviceId.isEmpty()) throw new IOException("Movistar Plus new device: empty id");
        Map<String, String> headers = jsonHeaders(true);
        headers.put("X-Movistarplus-Deviceid", deviceId);
        check(http.send("POST", base + encode(deviceId) + "?qspVersion=ssp", headers, ""), "device registration");
        selectDevice(deviceId);
    }

    List<Profile> profiles() throws IOException, AuthException {
        Session session = openSession();
        Map<String, String> headers = baseHeaders();
        headers.put("X-HZId", session.token);
        Http.Response response = http.send("GET", PROFILES, headers, null);
        check(response, "profiles");
        List<Profile> profiles = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(response.body).optJSONArray("items");
            for (int i = 0; items != null && i < items.length(); i++) {
                JSONObject profile = items.getJSONObject(i);
                profiles.add(new Profile(profile.getString("id"), profile.optString("name")));
            }
        } catch (JSONException e) {
            throw new IOException("Movistar Plus profiles: unreadable answer", e);
        }
        return profiles;
    }

    void selectProfile(String profileId) {
        secrets.put(PROFILE_ID, profileId);
    }

    String profileId() {
        String profile = secrets.get(PROFILE_ID);
        return profile == null ? MAIN_PROFILE : profile;
    }

    Session openSession() throws IOException, AuthException {
        refreshIfNeeded();
        String deviceId = secrets.get(DEVICE_ID);
        if (deviceId == null) throw new AuthException("Movistar Plus has no device");
        JSONObject body = new JSONObject();
        try {
            body.put("accountNumber", secrets.get(ACCOUNT));
            body.put("userProfile", profileId());
            body.put("streamMiscellanea", "HTTPS");
            body.put("deviceType", "SMARTTV_OTT");
            body.put("deviceManufacturerProduct", "LG");
            body.put("streamDRM", "Widevine");
            body.put("streamFormat", "DASH");
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        Map<String, String> headers = jsonHeaders(true);
        headers.put("X-Movistarplus-Deviceid", deviceId);
        Http.Response response = http.send("POST", CLIENT_SERVICES + "/" + DEVICE_CLASS + "/sdp/mediaPlayers/"
                + encode(deviceId) + "/initData?qspVersion=ssp&version=8&status=default", headers, body.toString());
        check(response, "initData");
        try {
            JSONObject json = new JSONObject(response.body);
            return new Session(json.getString("token"), json.optString("demarcation"));
        } catch (JSONException e) {
            throw new IOException("Movistar Plus initData: unreadable answer", e);
        }
    }

    void refreshIfNeeded() throws IOException, AuthException {
        if (secrets.get(RECONNECT_NEEDED) != null) throw new AuthException("Movistar Plus needs a new login");
        String token = secrets.get(ACCESS_TOKEN);
        if (token != null && parseLong(secrets.get(EXPIRES_AT)) - clock.getAsLong() > RENEW_MARGIN_MS) return;
        String user = secrets.get(USERNAME);
        String password = secrets.get(PASSWORD);
        if (user == null || password == null || requestToken(user, password) == null) throw rejected();
    }

    AuthException expired(String what) {
        secrets.remove(EXPIRES_AT);
        return new AuthException("Movistar Plus refused " + what + "; the next sync logs in again");
    }

    AuthException rejected() {
        secrets.put(RECONNECT_NEEDED, "1");
        return new AuthException("Movistar Plus rejected the session");
    }

    void disconnect() {
        for (String key : KEYS) secrets.remove(key);
    }

    Map<String, String> bearer() {
        Map<String, String> headers = baseHeaders();
        headers.put("Authorization", "Bearer " + secrets.get(ACCESS_TOKEN));
        return headers;
    }

    String account() {
        return secrets.get(ACCOUNT);
    }

    String platform() {
        return secrets.get(PLATFORM);
    }

    String user() {
        return secrets.get(USER);
    }

    private String requestToken(String user, String password) throws IOException {
        Map<String, String> headers = baseHeaders();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        String body = "grant_type=password&deviceClass=" + DEVICE_CLASS + "&username=" + encode(user)
                + "&password=" + encode(password);
        Http.Response response = http.send("POST", AUTH + "/auth/oauth2/token?deviceClass=" + DEVICE_CLASS, headers,
                body);
        if (response.code == 400 || response.code == 401) return null;
        if (!response.ok()) throw new IOException("Movistar Plus login: HTTP " + response.code);
        try {
            JSONObject json = new JSONObject(response.body);
            String token = json.getString("access_token");
            secrets.put(ACCESS_TOKEN, token);
            secrets.put(EXPIRES_AT, Long.toString(clock.getAsLong() + json.getLong("expires_in") * 1000));
            return token;
        } catch (JSONException e) {
            throw new IOException("Movistar Plus login: unreadable answer", e);
        }
    }

    private JSONObject accountInfo(String token) throws IOException {
        Map<String, String> headers = baseHeaders();
        headers.put("Authorization", "Bearer " + token);
        Http.Response response = http.send("GET", AUTH + "/movistarplus/api/devices/" + DEVICE_CLASS
                + "/users/authenticate?_=" + clock.getAsLong(), headers, null);
        if (!response.ok()) throw new IOException("Movistar Plus account: HTTP " + response.code);
        try {
            return new JSONObject(response.body);
        } catch (JSONException e) {
            throw new IOException("Movistar Plus account: unreadable answer", e);
        }
    }

    private void check(Http.Response response, String what) throws IOException, AuthException {
        if (response.code == 401 || response.code == 403) throw expired(what);
        if (!response.ok()) throw new IOException("Movistar Plus " + what + ": HTTP " + response.code);
    }

    private Map<String, String> jsonHeaders(boolean authorized) {
        Map<String, String> headers = authorized ? bearer() : baseHeaders();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    static Map<String, String> baseHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Language", "es-ES,es;q=0.9");
        headers.put("Origin", "https://ver.movistarplus.es");
        headers.put("Referer", "https://ver.movistarplus.es/");
        return headers;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
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
