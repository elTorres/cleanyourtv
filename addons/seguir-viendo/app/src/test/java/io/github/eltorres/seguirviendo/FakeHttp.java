package io.github.eltorres.seguirviendo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

final class FakeHttp implements Http {
    final List<String> requests = new ArrayList<>();
    final List<String> bodies = new ArrayList<>();
    private final Deque<Response> answers = new ArrayDeque<>();

    FakeHttp answer(int code, String body) {
        answers.add(new Response(code, body));
        return this;
    }

    @Override
    public Response send(String method, String url, Map<String, String> headers, String body) {
        requests.add(method + " " + url);
        bodies.add(body);
        Response next = answers.poll();
        if (next == null) throw new AssertionError("unexpected request " + method + " " + url);
        return next;
    }
}
