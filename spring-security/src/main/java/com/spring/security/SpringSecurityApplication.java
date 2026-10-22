package com.spring.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.spring.security",
    "config",
    "controller",
    "service",
    "repository",
    "security"
})
@EntityScan(basePackages = "entity")
@EnableJpaRepositories(basePackages = "repository")
@RestController
public class SpringSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityApplication.class, args);
    }

    @GetMapping("/")
    public String greet() {
        return "hey";
    }
}
