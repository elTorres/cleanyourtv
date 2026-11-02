package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MovistarAuthTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long TEN_DAYS_S = 864_000;
    private static final String TOKEN = "{\"access_token\":\"jwt-1\",\"token_type\":\"bearer\",\"expires_in\":" + TEN_DAYS_S + "}";
    private static final String ACCOUNT = "{\"cod_usuario_cifrado\":\"enc-user\",\"ofertas\":[{\"accountNumber\":\"00000001\","
            + "\"@id_perfil\":\"OTT\"}]}";
    private static final String DEVICES = "[{\"Id\":\"-\",\"Name\":\"\",\"DeviceType\":\"\"},"
            + "{\"Id\":\"dev-1\",\"Name\":\"Salón\",\"DeviceType\":\"Amazon TV\",\"DeviceTypeCode\":\"AMZTV\"}]";
    private static final String BAD_GRANT = "{\"error\":\"invalid_grant\"}";

    private final AtomicLong clock = new AtomicLong(NOW);
    private final MemorySecretStore secrets = new MemorySecretStore();

    private MovistarAuth auth(FakeHttp http) {
        return new MovistarAuth(http, secrets, clock::get);
    }

    @Test
    public void validLoginConnects() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, TOKEN).answer(200, ACCOUNT).answer(200, DEVICES);
        MovistarAuth auth = auth(http);
        assertEquals(MovistarAuth.LoginResult.CONNECTED, auth.login("persona@example.com", "clave"));
        assertTrue(http.bodies.get(0).contains("grant_type=password"));
        assertTrue(http.bodies.get(0).contains("username=persona%40example.com"));
        assertEquals("Bearer jwt-1", http.headers.get(1).get("Authorization"));
        assertEquals("00000001", auth.account());
        assertEquals("OTT", auth.platform());
        assertEquals("enc-user", auth.user());
        assertEquals("clave", secrets.get(MovistarAuth.PASSWORD));
        assertEquals(SourceState.DISCONNECTED, auth.state());

        List<MovistarAuth.Device> devices = auth.devices();
        auth.selectDevice(devices.get(0).id);
        assertEquals(SourceState.CONNECTED, auth.state());
    }

    @Test
    public void badPasswordStoresNothing() throws Exception {
        MovistarAuth auth = auth(new FakeHttp().answer(401, BAD_GRANT));
        assertEquals(MovistarAuth.LoginResult.BAD_PASSWORD, auth.login("persona@example.com", "mal"));
        assertTrue(secrets.values.isEmpty());
        assertEquals(SourceState.DISCONNECTED, auth.state());
    }

    @Test
    public void reusesARegisteredDevice() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, TOKEN).answer(200, ACCOUNT).answer(200, DEVICES);
        MovistarAuth auth = auth(http);
        auth.login("persona@example.com", "clave");
        List<MovistarAuth.Device> devices = auth.devices();
        assertEquals(1, devices.size());
        assertEquals("dev-1", devices.get(0).id);
        assertEquals("Amazon TV", devices.get(0).type);
        auth.selectDevice("dev-1");
        assertEquals("dev-1", secrets.get(MovistarAuth.DEVICE_ID));
        assertEquals(3, http.requests.size());
        assertTrue(http.requests.stream().noneMatch(r -> r.startsWith("POST") && r.contains("/devices")));
    }

    @Test
    public void registrationAsksBeforeTakingADeviceSlot() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, TOKEN).answer(200, ACCOUNT).answer(200, "[]");
        MovistarAuth auth = auth(http);
        auth.login("persona@example.com", "clave");
        assertTrue(auth.devices().isEmpty());
        assertEquals(SourceState.DISCONNECTED, auth.state());
        assertTrue(http.requests.stream().noneMatch(r -> r.startsWith("POST") && r.contains("/devices")));

        http.answer(200, "\"new-dev\"").answer(201, "");
        auth.registerDevice();
        assertEquals("new-dev", secrets.get(MovistarAuth.DEVICE_ID));
        assertTrue(http.requests.get(4).endsWith("/devices/new-dev?qspVersion=ssp"));
        assertEquals(SourceState.CONNECTED, auth.state());
    }

    @Test
    public void rejectedRefreshMarksReconnectNeeded() throws Exception {
        connected(NOW - 1);
        MovistarAuth auth = auth(new FakeHttp().answer(401, BAD_GRANT));
        try {
            auth.openSession();
            fail("a rejected login must throw");
        } catch (AuthException expected) {
            assertEquals(SourceState.RECONNECT_NEEDED, auth.state());
        }
    }

    @Test
    public void expiringTokenLogsInAgainWithTheStoredPassword() throws Exception {
        connected(NOW + 60_000);
        FakeHttp http = new FakeHttp().answer(200, TOKEN).answer(200, "{\"token\":\"hz-1\",\"demarcation\":\"28\"}");
        MovistarAuth.Session session = auth(http).openSession();
        assertEquals("hz-1", session.token);
        assertEquals("28", session.demarcation);
        assertTrue(http.bodies.get(0).contains("password=clave"));
        assertEquals("dev-1", http.headers.get(1).get("X-Movistarplus-Deviceid"));
        assertTrue(http.bodies.get(1).contains("\"userProfile\":\"0\""));
    }

    @Test
    public void refusedSessionLogsInAgainOnTheNextSync() throws Exception {
        connected(NOW + TEN_DAYS_S * 1000);
        FakeHttp http = new FakeHttp().answer(401, "{\"Message\":\"Unauthorized\"}");
        MovistarAuth auth = auth(http);
        try {
            auth.openSession();
            fail("a refused session must throw");
        } catch (AuthException expected) {
            assertEquals(SourceState.CONNECTED, auth.state());
        }
        http.answer(200, TOKEN).answer(200, "{\"token\":\"hz-2\"}");
        assertEquals("hz-2", auth.openSession().token);
    }

    @Test
    public void selectedProfileOpensTheSession() throws Exception {
        connected(NOW + TEN_DAYS_S * 1000);
        FakeHttp http = new FakeHttp().answer(200, "{\"token\":\"hz-1\"}")
                .answer(200, "{\"items\":[{\"id\":\"0\",\"name\":\"Hogar\"},{\"id\":\"2\",\"name\":\"Segundo\"}]}");
        MovistarAuth auth = auth(http);
        List<MovistarAuth.Profile> profiles = auth.profiles();
        assertEquals(2, profiles.size());
        assertEquals("hz-1", http.headers.get(1).get("X-HZId"));
        auth.selectProfile(profiles.get(1).id);
        http.answer(200, "{\"token\":\"hz-2\"}");
        auth.openSession();
        assertTrue(http.bodies.get(2).contains("\"userProfile\":\"2\""));
    }

    @Test
    public void disconnectForgetsEverything() {
        connected(NOW + TEN_DAYS_S * 1000);
        auth(new FakeHttp()).disconnect();
        assertTrue(secrets.values.isEmpty());
        assertNull(secrets.get(MovistarAuth.PASSWORD));
    }

    private void connected(long expiresAt) {
        secrets.put(MovistarAuth.USERNAME, "persona@example.com");
        secrets.put(MovistarAuth.PASSWORD, "clave");
        secrets.put(MovistarAuth.ACCESS_TOKEN, "jwt-0");
        secrets.put(MovistarAuth.EXPIRES_AT, Long.toString(expiresAt));
        secrets.put(MovistarAuth.ACCOUNT, "00000001");
        secrets.put(MovistarAuth.PLATFORM, "OTT");
        secrets.put(MovistarAuth.USER, "enc-user");
        secrets.put(MovistarAuth.DEVICE_ID, "dev-1");
    }
}
