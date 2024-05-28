package com.axonivy.connector.vertexai.entities;

import com.google.gson.annotations.SerializedName;

public class InlineData {
	@SerializedName("mime_type")
	private String type;

	@SerializedName("data")
	private String base64Data;
	
    public InlineData(String type, String base64Data) {
        this.type = type;
        this.base64Data = base64Data;
    }

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBase64Data() {
		return base64Data;
	}

	public void setBase64Data(String base64Data) {
		this.base64Data = base64Data;
	}
}
