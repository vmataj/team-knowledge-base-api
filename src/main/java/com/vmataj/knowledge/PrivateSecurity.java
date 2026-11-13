package com.vmataj.knowledge;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class PrivateSecurity {
    @Bean
    SecurityFilterChain privateAccess(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login.html", "/signup.html", "/index.html", "/style.css", "/account.js", "/planner.js", "/account", "/account/signup", "/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")))
            .formLogin(login -> login.loginPage("/login.html").loginProcessingUrl("/login")
                .failureUrl("/login.html?error").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/"))
            .build();
    }

    @Bean
    UserDetailsManager users(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }
}
