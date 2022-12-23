package com.portfolio.backend.model;

import java.io.Serializable;

public class JwtResponse  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7894727372821824664L;
	
	private final String jwt_token;
	private final String username;
	
	public JwtResponse(String jwt_token, String username) {
		this.jwt_token = jwt_token;
		this.username = username;
	}
	
	public String getToken() {
		return this.jwt_token;
	}
	public String getUsername() {
		return this.username;
	}

}
