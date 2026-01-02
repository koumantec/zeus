package com.stetits.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class CoreControlApplicationTests {

	static String dbPath;

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) throws Exception {
		dbPath = "target/test-" + java.util.UUID.randomUUID() + ".db";
		registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbPath);
	}

	@AfterAll
	static void cleanup() throws Exception {
		java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(dbPath));
	}

	@Test
	void contextLoads() {
	}

}
