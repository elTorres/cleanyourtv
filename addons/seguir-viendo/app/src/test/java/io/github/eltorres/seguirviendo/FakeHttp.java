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
    private final Deque<byte[]> images = new ArrayDeque<>();

    FakeHttp answer(int code, String body) {
        answers.add(new Response(code, body));
        return this;
    }

    FakeHttp image(byte[] bytes) {
        images.add(bytes);
        return this;
    }

    @Override
    public byte[] download(String url, int maxBytes) {
        requests.add("GET " + url);
        byte[] next = images.poll();
        if (next == null) throw new AssertionError("unexpected download " + url);
        return next;
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
