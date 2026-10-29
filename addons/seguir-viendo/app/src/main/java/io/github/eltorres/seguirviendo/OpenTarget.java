package io.github.eltorres.seguirviendo;

final class OpenTarget {
    private final String targetPackage;
    private final String deepLink;

    OpenTarget(String targetPackage, String deepLink) {
        this.targetPackage = targetPackage;
        this.deepLink = deepLink;
    }

    String targetPackage() {
        return targetPackage;
    }

    String deepLink() {
        return deepLink;
    }
}
