package com.vmataj.knowledge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Profile("!private")
class DemoSecurity {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/demo.js", "/account.js", "/style.css", "/account", "/error").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .httpBasic(withDefaults())
            .build();
    }

    @Bean
    UserDetailsManager users() {
        String password = new BCryptPasswordEncoder().encode("demo-pass");
        return new InMemoryUserDetailsManager(
            User.withUsername("editor").password("{bcrypt}" + password).roles("USER").build(),
            User.withUsername("viewer").password("{bcrypt}" + password).roles("USER").build(),
            User.withUsername("outsider").password("{bcrypt}" + password).roles("USER").build()
        );
    }
}
