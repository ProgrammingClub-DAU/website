package com.cpclub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the CP Club backend.
 *
 * <p>Enabling scheduling here activates the Codeforces synchronization job while
 * Spring Boot discovers the feature-oriented controllers, services, repositories,
 * security components, and shared infrastructure under this package.</p>
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	/**
	 * Starts the Spring Boot application and its embedded HTTP server.
	 *
	 * @param args command-line arguments supplied by the runtime
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
