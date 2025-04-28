package com.wordle.quiz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Slf4j
@EnableRedisHttpSession
@EnableJpaAuditing
@SpringBootApplication
public class QuizApplication {

	public static void main(String[] args) {
		log.info("REDIS_HOST from environment: {}", System.getenv("REDIS_HOST"));
		log.info("REDIS_PORT from environment: {}", System.getenv("REDIS_PORT"));
		SpringApplication.run(QuizApplication.class, args);
	}

}
