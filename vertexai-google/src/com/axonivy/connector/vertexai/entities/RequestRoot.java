package com.axonivy.connector.vertexai.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RequestRoot {
	@SerializedName("contents")
	private List<Content> contents;

	public RequestRoot(List<Content> contents) {
		this.contents = contents;
	}

	public List<Content> getContents() {
		return contents;
	}

	public void setContents(List<Content> contents) {
		this.contents = contents;
	}

}
