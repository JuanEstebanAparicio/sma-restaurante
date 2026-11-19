package sma.restaurante.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestauranteWebApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                RestauranteWebApplication.class,
                args
        );
    }
}