package com.saas.automotriz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AutomotrizApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomotrizApplication.class, args);
	}

}
