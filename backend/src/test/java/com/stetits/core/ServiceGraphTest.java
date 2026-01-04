package com.stetits.core;

import com.stetits.core.stack.ServiceGraph;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class ServiceGraphTest {

    @Test
    void topoSort_empty_returns_empty() {
        Map<String, List<String>> deps = new LinkedHashMap<>();
        assertThat(ServiceGraph.topoSort(deps)).isEmpty();
    }

    @Test
    void topoSort_single_node_no_deps() {
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("web", List.of());

        assertThat(ServiceGraph.topoSort(deps)).containsExactly("web");
    }

    @Test
    void topoSort_simple_chain() {
        // web depends_on db => db then web
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("db", List.of());
        deps.put("web", List.of("db"));

        assertThat(ServiceGraph.topoSort(deps)).containsExactly("db", "web");
    }

    @Test
    void topoSort_dependency_not_declared_as_key_is_still_included() {
        // web depends_on db, but db isn't present as a top-level key
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("web", List.of("db"));

        // Algorithm adds "db" as a node with indegree 0, so it must appear before "web"
        assertThat(ServiceGraph.topoSort(deps)).containsExactly("db", "web");
    }

    @Test
    void topoSort_diamond_shape() {
        //   db
        //  /  \
        // api  cache
        //  \  /
        //   web
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("db", List.of());
        deps.put("api", List.of("db"));
        deps.put("cache", List.of("db"));
        deps.put("web", List.of("api", "cache"));

        List<String> order = ServiceGraph.topoSort(deps);

        // Must contain all nodes exactly once
        assertThat(order).containsExactlyInAnyOrder("db", "api", "cache", "web");
        assertThat(new HashSet<>(order)).hasSize(4);

        // Must respect constraints
        assertThat(order.indexOf("db")).isLessThan(order.indexOf("api"));
        assertThat(order.indexOf("db")).isLessThan(order.indexOf("cache"));
        assertThat(order.indexOf("api")).isLessThan(order.indexOf("web"));
        assertThat(order.indexOf("cache")).isLessThan(order.indexOf("web"));
    }

    @Test
    void topoSort_disconnected_components_all_included() {
        // Two components: (db -> web) and (mq) alone
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("db", List.of());
        deps.put("web", List.of("db"));
        deps.put("mq", List.of());

        List<String> order = ServiceGraph.topoSort(deps);

        assertThat(order).containsExactlyInAnyOrder("db", "web", "mq");
        assertThat(order.indexOf("db")).isLessThan(order.indexOf("web"));
    }

    @Test
    void topoSort_cycle_throws() {
        // a -> b -> a
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("a", List.of("b"));
        deps.put("b", List.of("a"));

        assertThatThrownBy(() -> ServiceGraph.topoSort(deps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle detected");
    }

    @Test
    void topoSort_self_cycle_throws() {
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("a", List.of("a"));

        assertThatThrownBy(() -> ServiceGraph.topoSort(deps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle detected");
    }

    @Test
    void topoSort_multiple_zero_indegree_nodes_is_deterministic_with_linkedhashmap() {
        // With LinkedHashMap insertion order, "a" then "b" are both indegree 0
        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("a", List.of());
        deps.put("b", List.of());
        deps.put("c", List.of("a", "b"));

        List<String> order = ServiceGraph.topoSort(deps);

        // In this implementation, queue is built by iterating inDegree.entrySet()
        // which follows insertion order for LinkedHashMap keys created in the code path.
        // Expected: a, b, c
        assertThat(order).containsExactly("a", "b", "c");
    }
}
