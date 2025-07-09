package io.hhplus;

public enum PointAction {
    CHARGE("/point/{id}/charge"),
    USE("/point/{id}/use"),
    DELIVER("/point/{id}/deliver");

    private final String path;

    PointAction(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}