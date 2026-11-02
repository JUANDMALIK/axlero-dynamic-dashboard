package com.axlero.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import dashboard_builder.config.SecurityConfig;

@SpringBootApplication
@EnableCaching
@Import(SecurityConfig.class)
public class DynamicDataDashboardBuilderApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicDataDashboardBuilderApplication.class, args);
	}

}

