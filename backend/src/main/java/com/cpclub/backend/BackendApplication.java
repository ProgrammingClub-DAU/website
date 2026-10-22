package com.cpclub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

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
		// Every Phase 3 timestamp column holds UTC (D31), but LocalDateTime.now(),
		// @CreationTimestamp and JDBC Timestamp binding all use the JVM's zone. That
		// is UTC on Render and IST on a laptop, so a sync run from a laptop wrote
		// times five and a half hours off. Pinning the zone makes both agree.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
