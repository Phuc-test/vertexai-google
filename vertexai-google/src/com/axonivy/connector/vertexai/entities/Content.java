package com.axonivy.connector.vertexai.entities;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Content {
	@SerializedName("role")
	private String role;

	@SerializedName("parts")
	private List<Part> parts;

	public Content(String role, List<Part> parts) {
		this.role = role;
		this.parts = parts;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public List<Part> getParts() {
		return parts;
	}

	public void setParts(List<Part> parts) {
		this.parts = parts;
	}
}
