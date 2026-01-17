package ru.binarysimple.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import ru.binarysimple.order.filter.UserAuthFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfiguration {

    private final UserAuthFilter userAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/order/**").authenticated()
                .requestMatchers("/actuator/**").permitAll()
                // Разрешаем Swagger UI и API docs
                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error"
                ).permitAll()
                .anyRequest().authenticated());
        http.headers(Customizer.withDefaults());
        http.anonymous(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);

        configureFilters(http);

        return http.build();
    }

    private void configureFilters(HttpSecurity http) {
        http.addFilterBefore(userAuthFilter, AuthorizationFilter.class);
    }
}