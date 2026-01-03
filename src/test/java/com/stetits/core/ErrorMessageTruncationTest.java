package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandWorker;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ErrorMessageTruncationTest extends TestBase {
    @Autowired CommandsRepository commands;
    @Autowired CommandWorker worker;

    @TestConfiguration
    static class TestHandlersConfig {
        @Bean
        public CommandHandler explodingHandler() {
            CommandHandler handler = mock(CommandHandler.class);
            when(handler.type()).thenReturn("EXPLODE_LONG");
            
            try {
                doAnswer(invocation -> {
                    CommandContext ctx = invocation.getArgument(0);
                    String longMsg = "X".repeat(5000);
                    ctx.error("About to throw long exception message of length=" + longMsg.length());
                    throw new RuntimeException(longMsg);
                }).when(handler).execute(any(CommandContext.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            return handler;
        }
    }

    @Test
    void worker_truncates_error_message_to_2000_chars() throws Exception {
        long id = commands.enqueue("s1", "EXPLODE_LONG", "{}");

        boolean processed = worker.processOne();
        assertThat(processed).isTrue();

        var cmd = commands.get(id).orElseThrow();
        assertThat(cmd.status()).isEqualTo("FAILED");
        assertThat(cmd.errorMessage()).isNotNull();
        assertThat(cmd.errorMessage().length()).isLessThanOrEqualTo(2000);
    }
}
