package com.danver.messengerserver.configs;

import com.danver.messengerserver.filters.ChatsAuthorizationFilter;
import com.danver.messengerserver.filters.JwtTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final ChatsAuthorizationFilter chatsAuthorizationFilter;

    private final String[] urlWhiteList = {
            "/",
            "/ws",
            "/ws/**",
            "/auth/**",
            "/config/**",
            "/public/**"
    };

    @Autowired
    public SecurityConfig(JwtTokenFilter jwtTokenFilter, ChatsAuthorizationFilter chatsAuthorizationFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.chatsAuthorizationFilter = chatsAuthorizationFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowCredentials(true);
        //TODO: we should allow all headers
        //configuration.setExposedHeaders(List.of(*));
        configuration.setExposedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.WWW_AUTHENTICATE,
                HttpHeaders.SET_COOKIE
        ));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
        return http.cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(this.urlWhiteList).permitAll()
                        //.requestMatchers("/chats/**").access(new ChatsAuthorizationManager())
                        .anyRequest()
                        .authenticated()
                )
                .headers((headers) -> {
                })
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class) // or addFilterAt - instead of some filter
                .addFilterBefore(chatsAuthorizationFilter, AuthorizationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                // Custom authentication failure handler (implements AuthenticationEntryPoint).
                                // Here we return 401 code instead of default 403
                                (request, response, exception) -> {
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                                }
                        )
/*                        .accessDeniedHandler((request, response, exception) -> {
                        // Custom authorization failure handler (implements AccessDeniedHandler).
                        // Here we return 403 code
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
                        })*/
                )
                .build();
    }
}
