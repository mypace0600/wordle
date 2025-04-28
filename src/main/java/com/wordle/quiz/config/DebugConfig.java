package com.wordle.quiz.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DebugConfig {

    @Bean
    ApplicationRunner debugRunner() {
        return args -> {
            log.info("REDIS_HOST from environment: {}", System.getenv("REDIS_HOST"));
            log.info("REDIS_PORT from environment: {}", System.getenv("REDIS_PORT"));
            log.info("REDIS_HOST from property: {}", System.getProperty("spring.data.redis.host"));
            log.info("REDIS_PORT from property: {}", System.getProperty("spring.data.redis.port"));
        };
    }
}
