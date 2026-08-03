package com.shuld.jac.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * UserDetailsServiceAutoConfiguration is excluded: authorization here is entirely JWT-based
 * (httpBasic/formLogin are disabled in SecurityConfig), so Boot's default in-memory user would
 * otherwise be created and its generated password logged on every startup for a credential
 * that is never actually checked.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(basePackages = {"com.shuld.jac.user_service", "com.shuld.jac.jwtcommon"})
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
