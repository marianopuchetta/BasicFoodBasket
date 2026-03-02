package com.basic_food_basket.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Lazy
	@Autowired
	private UserDetailsService jwtUserDetailsService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		// Use BCryptPasswordEncoder
		auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoder());
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
				// We don't need CSRF for APIs with JWT
				.csrf().disable()
				.cors().and()

				.authorizeRequests()

				// Endpoints públicos
				.antMatchers(
						"/authenticate",
						"/register",
						"/home",

						// Permitir todos los endpoints de canasta
						"/canasta/**",

						// Permitir todos los endpoints del scraper
						"/scrap/**"
				).permitAll()

				// ====== REGLAS ROLE_ADMIN ======
				// Solo ADMIN puede crear productos
				.antMatchers(HttpMethod.POST, "/productos/newproducto").hasAuthority("ROLE_ADMIN")

				// (opcional) si querés que cualquier modificación de productos sea admin:
				.antMatchers(HttpMethod.POST, "/productos/**").hasAuthority("ROLE_ADMIN")
				 .antMatchers(HttpMethod.PUT, "/productos/**").hasAuthority("ROLE_ADMIN")
				.antMatchers(HttpMethod.PATCH, "/productos/**").hasAuthority("ROLE_ADMIN")
				 .antMatchers(HttpMethod.DELETE, "/productos/**").hasAuthority("ROLE_ADMIN")

				// Todo lo demás requiere estar autenticado
				.anyRequest().authenticated().and()

				.exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// Validar JWT en cada request
		httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// Vite dev server
		configuration.setAllowedOrigins(Arrays.asList(
				"http://localhost:5173"
		));

		configuration.setAllowCredentials(true);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
		configuration.setExposedHeaders(Arrays.asList("token"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}