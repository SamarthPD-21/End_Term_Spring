package com.example.endtrem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EndtremApplication {

	public static void main(String[] args) {
		SpringApplication.run(EndtremApplication.class, args);
	}

}
