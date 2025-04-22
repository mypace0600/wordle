package com.wordle.quiz.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckController {

    @GetMapping("/health-check")
        public ResponseEntity<String> health() {
            return ResponseEntity.ok("OK");
    }

}
