package com.nayan.obai.inventory.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig
{
	final Logger logger = LogManager.getLogger("SecurityConfig");
	@Bean
	public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception
	{
		logger.info("Setting filters for security config");
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/login**", "/oauth2/**", "/actuator/**").permitAll()
						.requestMatchers("/inventory/**").authenticated()
						.anyRequest().permitAll()
				)
//				.oauth2Login(Customizer.withDefaults()) // Auth0 login flow
//				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())); // JWT token validation
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))); // JWT token validation for custom converter to convert claims to roles

		return http.build();
	}

	// jwt converter for adding claims as roles
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		logger.info("Setting jwt converter");
		final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			final List<GrantedAuthority> authorities = new ArrayList<>();

			// This claim is same as we configure in okta under Security -> API -> Authorization Server -> default -> Claims tab
			final List<String> roles = jwt.getClaimAsStringList("MyClaim");
//
			logger.info("Roles are:" + roles);
			logger.info("Claims are:" + jwt.getClaims());
			if (roles != null)
			{
				// this will convert group Admin to ROLE_ADMIN, Regular Users to ROLE_REGULAR_USERS
				// so you can use hasRole('ADMIN') and hasRole('REGULAR_USERS')
				authorities.addAll(roles.stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace(" ", "_")))
						.collect(Collectors.toList()));
			}
			// Scopes from machine tokens
			final List<String> scopes = jwt.getClaimAsStringList("scp");
			logger.info("Scopes are:" + scopes);
			if (scopes != null) {
				authorities.addAll(scopes.stream()
						.map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
						.collect(Collectors.toList()));
			}
			return authorities;
		});

		return converter;
	}

}
