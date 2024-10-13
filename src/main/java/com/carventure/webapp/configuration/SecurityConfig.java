package com.carventure.webapp.configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/api/users/generatePhoneOtp", "/api/users/verifyPhoneOtp").permitAll() // Allow these endpoints
                .anyRequest().authenticated() // Require authentication for all other requests
                .and()
                .addFilter(new JwtAuthorizationFilter(authenticationManager())) // Your custom filter
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS); // Use stateless session management
    }

    // Remove the authenticationManagerBean() method completely if it’s causing issues
}
