package com.dissayakesuper.web_pos_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WebPosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebPosBackendApplication.class, args);
	}

}
