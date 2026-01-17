package com.catanatron.core.model;

import java.util.Objects;

public final class Edge {
    private final int a;
    private final int b;

    public Edge(int u, int v) {
        if (u <= v) { this.a = u; this.b = v; }
        else { this.a = v; this.b = u; }
    }

    public int a() { return a; }
    public int b() { return b; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge edge)) return false;
        return a == edge.a && b == edge.b;
    }

    @Override public int hashCode() { return Objects.hash(a, b); }

    @Override public String toString() { return "("+a+","+b+")"; }
}

