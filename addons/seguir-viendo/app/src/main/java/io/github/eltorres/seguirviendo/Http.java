package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.Map;

interface Http {
    final class Response {
        final int code;
        final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }

        boolean ok() {
            return code >= 200 && code < 300;
        }
    }

    Response send(String method, String url, Map<String, String> headers, String body) throws IOException;
}
