package com.knowledgeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KnowledgeAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeAppApplication.class, args);
    }
}
