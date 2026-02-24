package com.leaveease.leaveease_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LeaveEaseApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveEaseApiApplication.class, args);
	}

}
