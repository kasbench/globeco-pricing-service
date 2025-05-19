package org.kasbench.globeco_pricing_service;

import org.springframework.boot.SpringApplication;

public class TestGlobecoPricingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(GlobecoPricingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
