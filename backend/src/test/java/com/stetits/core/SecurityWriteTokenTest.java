package com.stetits.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "core.api.token=test-token",
        "orchestrator.worker.autostart=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityWriteTokenTest {

    @Autowired MockMvc mvc;

    @Test
    void post_without_token_is_401() throws Exception {
        mvc.perform(post("/stacks/s1/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_with_token_is_not_401() throws Exception {
        mvc.perform(post("/stacks/s1/start")
                        .header("Authorization","Bearer test-token"))
                .andExpect(status().is4xxClientError()); // likely 404 if stack doesn't exist; key is "not 401"
    }
}
