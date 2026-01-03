package com.stetits.core;

import com.stetits.core.stack.StackSpecParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StackSpecParserTest {

    @Test
    void parse_reads_services_ports_env_deps_volumes() throws Exception {
        String body = """
      {"compose":{"services":{
        "db":{"image":"redis:7","volumes":["data:/data"]},
        "web":{"image":"nginx:alpine","ports":["8080:80"],"depends_on":["db"],"environment":{"A":"B"}}
      },"volumes":{"data":{}}}}
      """;

        var parser = new StackSpecParser();
        var spec = parser.parse(body);

        assertThat(spec.services()).containsKeys("db","web");
        assertThat(spec.services().get("web").dependsOn()).containsExactly("db");
        assertThat(spec.services().get("web").ports()).containsExactly("8080:80");
        assertThat(spec.volumes()).containsKey("data");
    }

    @Test
    void parse_throws_if_no_services() {
        String body = "{\"compose\":{\"services\":{}}}";
        var parser = new StackSpecParser();
        assertThatThrownBy(() -> parser.parse(body))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
