package com.autobots.automanager.configuracao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.autobots.automanager.adaptadores.UserDetailsServiceImpl;
import com.autobots.automanager.filtros.Autenticador;
import com.autobots.automanager.filtros.Autorizador;
import com.autobots.automanager.jwt.ProvedorJwt;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class Seguranca extends WebSecurityConfigurerAdapter {

	@Autowired
	private UserDetailsServiceImpl servico;

	@Autowired
	private ProvedorJwt provedorJwt;

	private static final String[] rotasPublicas = { "/login" };

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.cors().and().csrf().disable();

		http.authorizeHttpRequests().antMatchers(rotasPublicas).permitAll().anyRequest().authenticated();

		http.addFilter(new Autenticador(authenticationManager(), provedorJwt));
		http.addFilter(new Autorizador(authenticationManager(), provedorJwt, servico));

		// sem token válido -> 401 (e não 403)
		http.exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}

	@Override
	protected void configure(AuthenticationManagerBuilder autenticador) throws Exception {
		autenticador.userDetailsService(servico).passwordEncoder(new BCryptPasswordEncoder());
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		CorsConfiguration cors = new CorsConfiguration().applyPermitDefaultValues();
		cors.addAllowedMethod("*"); // PUT e DELETE também
		cors.addExposedHeader("Authorization"); // o JWT volta neste cabeçalho no login
		fonte.registerCorsConfiguration("/**", cors);
		return fonte;
	}
}
