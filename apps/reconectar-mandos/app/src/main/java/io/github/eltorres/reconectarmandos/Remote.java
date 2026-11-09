package io.github.eltorres.reconectarmandos;

import java.util.Objects;

final class Remote {
    private final String address;
    private final String name;

    Remote(String address, String name) {
        this.address = address;
        this.name = name == null || name.isEmpty() ? address : name;
    }

    String address() {
        return address;
    }

    String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Remote && ((Remote) other).address.equals(address) && ((Remote) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, name);
    }
}
