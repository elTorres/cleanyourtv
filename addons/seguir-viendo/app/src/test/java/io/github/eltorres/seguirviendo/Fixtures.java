package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

final class Fixtures {
    static final long NOW = Instant.parse("2027-01-16T12:00:00Z").toEpochMilli();

    private Fixtures() {
    }

    static String read(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources", name)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static TraktAuth connectedTrakt() {
        MemorySecretStore secrets = new MemorySecretStore();
        secrets.put(TraktAuth.ACCESS_TOKEN, "access");
        secrets.put(TraktAuth.REFRESH_TOKEN, "refresh");
        secrets.put(TraktAuth.EXPIRES_AT, Long.toString(Long.MAX_VALUE));
        return new TraktAuth(new FakeHttp(), secrets, "client", "", "http://127.0.0.1", () -> NOW);
    }
}
