package com.shuld.jac.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * UserDetailsServiceAutoConfiguration is excluded: every endpoint here is permitAll (this
 * service IS the authentication entry point), so Boot's default in-memory user would otherwise
 * be created and its generated password logged on every startup for a credential that is never
 * actually checked.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(basePackages = {"com.shuld.jac.authservice", "com.shuld.jac.jwtcommon"})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
