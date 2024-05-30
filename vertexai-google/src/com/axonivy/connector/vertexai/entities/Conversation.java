package com.axonivy.connector.vertexai.entities;

public class Conversation {
	private String role;
	private String text;

	public Conversation(String role, String text) {
		this.role = role;
		this.text = text;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
