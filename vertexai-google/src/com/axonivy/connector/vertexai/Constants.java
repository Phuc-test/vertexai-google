package com.axonivy.connector.vertexai;

public class Constants {
	public static final String COMMA = ",";
	public static final String onlyTextInput = """
			       {
			         "role": "%s",
			         "parts": [
			           {
			             "text": "%s"
			           }
			         ]
			       }
			""";

	public static final String onlyTextOutput = """
			       {
			         "role": "%s",
			         "parts": [
			           {
			             "text": %s
			           }
			         ]
			       }
			""";

	public static final String textAndImageInput = """
			       {
			         "role": "%s",
			         "parts": [
			           {
			             "text": "%s"
			           },
			           %s
			         ]
			       }
			""";

	public static final String jsonContent = """
			    {
			        "contents": [%s]
			    }
			""";

	public static final String imageInput = """
			    {
			      "inline_data": {
			        "mime_type": "image/jpeg",
			        "data": "%s"
			      }
			    }
			""";
}
