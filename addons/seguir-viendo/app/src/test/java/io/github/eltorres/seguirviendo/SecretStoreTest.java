package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SecretStoreTest {

    @Test
    public void neverLogsSecrets() {
        MemorySecretStore secrets = new MemorySecretStore();
        secrets.put(TraktAuth.ACCESS_TOKEN, "tok-abc123");
        secrets.put(TraktAuth.REFRESH_TOKEN, "ref-xyz789");
        String line = secrets.scrub("failed with tok-abc123 and ref-xyz789, header Authorization: Bearer other-token");
        assertFalse(line.contains("tok-abc123"));
        assertFalse(line.contains("ref-xyz789"));
        assertFalse(line.contains("other-token"));
        assertEquals("plain message", secrets.scrub("plain message"));
    }
}
