package com.axonivy.connector.vertexai.enums;

public enum Model {
	VERTEXAI_GEMINI("VertexAi-Gemini"), GEMINI("Gemini");

	private String name;

	private Model(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
