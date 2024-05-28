package com.axonivy.connector.vertexai.entities;

import com.google.gson.annotations.SerializedName;

public class Part {

	@SerializedName("text")
	private String text;

	@SerializedName("inline_data")
	private InlineData inlineData;

    public Part(String text) {
        this.text = text;
    }

    public Part(InlineData inlineData) {
        this.inlineData = inlineData;
    }
    
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public InlineData getInlineData() {
		return inlineData;
	}

	public void setInlineData(InlineData inlineData) {
		this.inlineData = inlineData;
	}
}
