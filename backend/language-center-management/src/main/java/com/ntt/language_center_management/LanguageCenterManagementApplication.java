package com.ntt.language_center_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LanguageCenterManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanguageCenterManagementApplication.class, args);
	}

}
