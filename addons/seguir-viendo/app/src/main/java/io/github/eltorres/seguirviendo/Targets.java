package io.github.eltorres.seguirviendo;

interface Targets {
    String targetPackage(String sourceId);

    void setTargetPackage(String sourceId, String packageName);
}
