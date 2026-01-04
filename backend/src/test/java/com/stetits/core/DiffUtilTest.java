package com.stetits.core;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.stack.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiffUtilTest {

    @Test
    void equalsSpec_true_when_all_match() {
        var actual = new DockerClientFacade.InspectContainer(
                "id", "nginx:alpine",
                Map.of("A","B"),
                Map.of(8080,80),
                "core_s1",
                List.of("web"),
                "web",
                List.of(new DockerClientFacade.MountSpec("core_s1_data","/data",false)),
                List.of("sh","-lc","sleep 300")
        );

        var desired = new DockerClientFacade.ContainerSpec(
                "core_s1_web","web","nginx:alpine",
                Map.of("A","B"),
                Map.of(8080,80),
                new DockerClientFacade.NetworkRef("nid","core_s1"),
                List.of("web"),
                List.of(new DockerClientFacade.MountSpec("core_s1_data","/data",false)),
                List.of("sh","-lc","sleep 300"),
                Map.of()
        );

        assertThat(DiffUtil.equalsSpec(actual, desired)).isTrue();
    }

    @Test
    void equalsSpec_false_on_hostname_mismatch() {
        var actual = new DockerClientFacade.InspectContainer(
                "id", "nginx:alpine", Map.of(), Map.of(), "core_s1", List.of("web"), "WRONG", List.of(), List.of("sh","-lc","sleep 300")
        );
        var desired = new DockerClientFacade.ContainerSpec(
                "core_s1_web","web","nginx:alpine", Map.of(), Map.of(),
                new DockerClientFacade.NetworkRef("nid","core_s1"), List.of("web"), List.of(), List.of("sh","-lc","sleep 300"), Map.of()
        );
        assertThat(DiffUtil.equalsSpec(actual, desired)).isFalse();
    }

    @Test
    void equalsSpec_false_on_command_mismatch() {
        var actual = new DockerClientFacade.InspectContainer(
                "id","alpine:3.20", Map.of(), Map.of(), "core_s1", List.of("app"), "app",
                List.of(), List.of("sh","-lc","sleep 300")
        );
        var desired = new DockerClientFacade.ContainerSpec(
                "core_s1_app","app","alpine:3.20", Map.of(), Map.of(),
                new DockerClientFacade.NetworkRef("nid","core_s1"),
                List.of("app"), List.of(), List.of("sh","-lc","sleep 999"), Map.of()
        );
        assertThat(DiffUtil.equalsSpec(actual, desired)).isFalse();
    }

}
