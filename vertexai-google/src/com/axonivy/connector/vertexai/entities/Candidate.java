package com.axonivy.connector.vertexai.entities;

import com.google.gson.annotations.SerializedName;

public class Candidate {
	@SerializedName("content")
	private Content content;

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}
}
