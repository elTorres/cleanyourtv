package io.github.eltorres.seguirviendo;

import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TraktAuthTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final String CODE = "{\"device_code\":\"dev\",\"user_code\":\"ABCD1234\","
            + "\"verification_url\":\"https://trakt.tv/activate\",\"expires_in\":600,\"interval\":5}";
    private static final String TOKEN = "{\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\","
            + "\"expires_in\":7776000,\"created_at\":1800000000}";

    private final AtomicLong clock = new AtomicLong(NOW);
    private final MemorySecretStore secrets = new MemorySecretStore();

    private TraktAuth auth(FakeHttp http, String clientId) {
        return new TraktAuth(http, secrets, clientId, "secret", "http://127.0.0.1", clock::get);
    }

    @Test
    public void worksWithoutClientSecretUsingPkce() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, CODE).answer(200, TOKEN);
        TraktAuth auth = new TraktAuth(http, secrets, "client", "", "http://127.0.0.1", clock::get);
        assertEquals(SourceState.DISCONNECTED, auth.state());
        TraktAuth.DeviceCode code = auth.requestCode();
        JSONObject codeBody = new JSONObject(http.bodies.get(0));
        assertEquals("S256", codeBody.getString("code_challenge_method"));
        assertEquals(Pkce.challenge(code.verifier), codeBody.getString("code_challenge"));
        assertEquals(TraktAuth.PollStatus.CONNECTED, auth.poll(code).status);
        JSONObject tokenBody = new JSONObject(http.bodies.get(1));
        assertEquals(code.verifier, tokenBody.getString("code_verifier"));
        assertFalse(tokenBody.has("client_secret"));
    }

    @Test
    public void challengeIsTheRfc7636Example() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    public void pollReturnsTokenAfterActivation() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, CODE).answer(400, "").answer(200, TOKEN);
        TraktAuth auth = auth(http, "client");
        TraktAuth.DeviceCode code = auth.requestCode();
        assertEquals("ABCD1234", code.userCode);
        assertEquals("https://trakt.tv/activate", code.verificationUrl);
        assertEquals(TraktAuth.PollStatus.PENDING, auth.poll(code).status);
        assertEquals(SourceState.DISCONNECTED, auth.state());
        assertEquals(TraktAuth.PollStatus.CONNECTED, auth.poll(code).status);
        assertEquals(SourceState.CONNECTED, auth.state());
        assertEquals("access-1", auth.accessToken());
    }

    @Test
    public void pollReportsExpiredCode() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, CODE).answer(410, "");
        TraktAuth auth = auth(http, "client");
        TraktAuth.DeviceCode code = auth.requestCode();
        assertEquals(TraktAuth.PollStatus.EXPIRED, auth.poll(code).status);
        clock.addAndGet(601_000);
        assertEquals(TraktAuth.PollStatus.EXPIRED, auth.poll(code).status);
        assertEquals(2, http.requests.size());
    }

    @Test
    public void missingClientIdMakesSourceUnavailable() {
        assertEquals(SourceState.UNAVAILABLE, auth(new FakeHttp(), "").state());
        assertEquals(SourceState.UNAVAILABLE, auth(new FakeHttp(), null).state());
    }

    @Test
    public void refreshRejectedMarksReconnectNeeded() throws Exception {
        secrets.put(TraktAuth.ACCESS_TOKEN, "old");
        secrets.put(TraktAuth.REFRESH_TOKEN, "old-refresh");
        secrets.put(TraktAuth.EXPIRES_AT, Long.toString(NOW + 1000));
        TraktAuth auth = auth(new FakeHttp().answer(401, ""), "client");
        try {
            auth.refreshIfNeeded();
            fail("a rejected refresh must throw");
        } catch (AuthException expected) {
            assertEquals(SourceState.RECONNECT_NEEDED, auth.state());
        }
    }

    @Test
    public void refreshSkippedWhileTokenIsFresh() throws Exception {
        secrets.put(TraktAuth.ACCESS_TOKEN, "a");
        secrets.put(TraktAuth.REFRESH_TOKEN, "r");
        secrets.put(TraktAuth.EXPIRES_AT, Long.toString(NOW + 30L * 24 * 60 * 60 * 1000));
        FakeHttp http = new FakeHttp();
        auth(http, "client").refreshIfNeeded();
        assertTrue(http.requests.isEmpty());
        assertNull(secrets.get(TraktAuth.RECONNECT_NEEDED));
    }
}
