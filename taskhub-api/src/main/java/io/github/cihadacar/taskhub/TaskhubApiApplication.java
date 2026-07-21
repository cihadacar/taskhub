package io.github.cihadacar.taskhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaskhubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskhubApiApplication.class, args);
    }
}
