package com.stetits.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.persistence.JdbcStackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.service.StackVersionService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StackVersionServiceTest {

    @Test
    void create_sets_parent_version_and_stores_sha() throws Exception {
        StacksRepository stacks = mock(StacksRepository.class);
        JdbcStackVersionsRepository versions = mock(JdbcStackVersionsRepository.class);

        when(stacks.get("s1")).thenReturn(Optional.of(new com.stetits.core.domain.dto.StackDto("s1","Stack",null)));
        when(versions.findLatestVersion("s1")).thenReturn(Optional.of("v123"));
        when(versions.findLatestBodySha("s1")).thenReturn(Optional.of("oldsha"));

        var svc = new StackVersionService(stacks, versions);
        var req = new StackVersionService.CreateVersionRequest(
                "v124",
                new ObjectMapper().readTree("{\"compose\":{\"services\":{\"web\":{\"image\":\"nginx:alpine\"}}}}"),
                "me",
                "test"
        );

        var res = svc.create("s1", req);

        assertThat(res.parentVersion()).isEqualTo("v123");
        assertThat(res.version()).isEqualTo("v124");
        assertThat(res.bodySha256()).isNotBlank();

        verify(versions, times(1)).insert(any());
    }

    @Test
    void create_does_not_insert_if_same_body_sha_as_latest() throws Exception {
        StacksRepository stacks = mock(StacksRepository.class);
        JdbcStackVersionsRepository versions = mock(JdbcStackVersionsRepository.class);
        ObjectMapper om = new ObjectMapper();

        when(stacks.get("s1")).thenReturn(Optional.of(new com.stetits.core.domain.dto.StackDto("s1","Stack",null)));
        when(versions.findLatestVersion("s1")).thenReturn(Optional.of("v1"));

        String body = "{\"a\":1}";
        // On force latest sha = sha(body)
        var svcTmp = new StackVersionService(stacks, versions);
        var sha = (String) StackVersionService.class.getDeclaredMethod("sha256Hex", String.class)
                .invoke(null, body); // reflection quick; sinon tu copies sha util dans test

        when(versions.findLatestBodySha("s1")).thenReturn(Optional.of(sha));
        when(versions.findLatestParentVersion("s1")).thenReturn(Optional.of("v0"));

        var svc = new StackVersionService(stacks, versions);
        var res = svc.create("s1", new StackVersionService.CreateVersionRequest(null, om.readTree(body), null, null));

        assertThat(res.version()).isEqualTo("v1");
        verify(versions, never()).insert(any());
    }
}
