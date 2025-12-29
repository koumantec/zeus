package com.stetits.core.stack;

import java.util.*;

public final class ServiceGraph {
    private ServiceGraph() {}

    public static List<String> topoSort(Map<String, List<String>> deps) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        for (var s : deps.keySet()) {
            inDegree.putIfAbsent(s, 0);
            adj.putIfAbsent(s, new ArrayList<>());
        }
        for (var e : deps.entrySet()) {
            String s = e.getKey();
            for (String d : e.getValue()) {
                inDegree.putIfAbsent(d, 0);
                adj.putIfAbsent(d, new ArrayList<>());
                adj.get(d).add(s);
                inDegree.put(s, inDegree.getOrDefault(s, 0) + 1);
            }
        }

        ArrayDeque<String> q = new ArrayDeque<>();
        for (var e : inDegree.entrySet()) if (e.getValue() == 0) q.add(e.getKey());

        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            String n = q.removeFirst();
            order.add(n);
            for (String to : adj.getOrDefault(n, List.of())) {
                int v = inDegree.merge(to, -1, Integer::sum);
                if (v == 0) q.add(to);
            }
        }
        if (order.size() != inDegree.size()) throw new IllegalArgumentException("Cycle detected in depends_on graph");
        return order;
    }
}
