package io.github.oldmanpushcart.moss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MossApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MossApplication.class, args);
    }

}
