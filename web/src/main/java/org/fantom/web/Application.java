package org.fantom.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

//    @Autowired
//    private Config config;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
