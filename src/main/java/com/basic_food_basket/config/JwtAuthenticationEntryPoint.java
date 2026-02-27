package com.basic_food_basket.config;


import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2133483377146044257L;

	@Override
	public void commence(HttpServletRequest request,
	                     HttpServletResponse response,
	                     AuthenticationException authException)
	        throws IOException {

	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    response.setContentType("application/json;charset=UTF-8");

	    String json = "{"
	            + "\"error\": \"Unauthorized\","
	            + "\"message\": \"Token missing or invalid\","
	            + "\"path\": \"" + request.getRequestURI() + "\""
	            + "}";

	    response.getWriter().write(json);
	}


}
