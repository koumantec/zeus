package com.stetits.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {DataJdbcRepositoriesAutoConfiguration.class})
public class CoreControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreControlApplication.class, args);
	}

}
