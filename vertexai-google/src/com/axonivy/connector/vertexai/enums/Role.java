package com.axonivy.connector.vertexai.enums;

public enum Role {
	USER("user"), MODEL("model");
	
	private String name;

	private Role(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
