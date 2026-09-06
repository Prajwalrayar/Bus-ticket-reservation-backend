package com.crimsonlogic.busticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusticketbookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusticketbookingApplication.class, args);
	}

}
