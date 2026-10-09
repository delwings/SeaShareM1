package com.seashare.seasharem1;

import org.springframework.boot.SpringApplication;

public class TestSeasharem1Application {

	public static void main(String[] args) {
		SpringApplication.from(Seasharem1Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
