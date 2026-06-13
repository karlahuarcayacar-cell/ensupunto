package com.ensupunto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EnsupuntoApplication {

	public static void main(String[] args) {
		java.util.Locale.setDefault(new java.util.Locale("es", "PE"));
		SpringApplication.run(EnsupuntoApplication.class, args);
	}

}
