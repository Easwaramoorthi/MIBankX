package com.mi.bankx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import lombok.extern.log4j.Log4j2;

@SpringBootApplication
@EntityScan(basePackages = "com.mi.bankx.model")
@EnableJpaRepositories(basePackages = "com.mi.bankx.repository")
@Log4j2
public class MiBankXApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiBankXApplication.class, args);
		log.info("MI Bank X Application Started Successfully");
	}

}
