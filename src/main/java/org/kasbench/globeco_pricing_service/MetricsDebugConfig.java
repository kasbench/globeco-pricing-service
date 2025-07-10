package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsDebugConfig {
    @Bean
    public CommandLineRunner logRegistries(MeterRegistry registry) {
        return args -> {
            System.out.println("MeterRegistry class: " + registry.getClass().getName());
            Counter counter = registry.counter("test.counter");
            counter.increment();
            System.out.println("Incremented test.counter metric");
        };
    }
} 