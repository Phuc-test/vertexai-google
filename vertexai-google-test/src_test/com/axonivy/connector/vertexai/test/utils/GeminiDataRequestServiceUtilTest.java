package com.axonivy.connector.vertexai.test.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.axonivy.connector.vertexai.entities.Content;
import com.axonivy.connector.vertexai.utils.GeminiDataRequestServiceUtils;
import com.google.gson.Gson;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class GeminiDataRequestServiceUtilTest {
	public static final String IMG_TAG_PATTERN = "<img\\s+[^>]*>";
	public static final String IMG_SRC_ATTR_PATTERN = "data:image\\/[^;]+;base64,([^\"]+)";

	private GeminiDataRequestServiceUtils geminiDataRequestServiceUtils = new GeminiDataRequestServiceUtils();

	@BeforeEach
	void beforeEach(AppFixture fixture) {
		fixture.var("vertexai-gemini.projectId", "generate-images-for-process");
		fixture.var("vertexai-gemini.location", "us-central");
		fixture.var("vertexai-gemini.modelName", "gemini-1.5-pro-preview-0409");
		fixture.var("vertexai-gemini.keyFilePath", "D:\\test.json");
		fixture.var("gemini.apiKey", "AIzaSyDaxbn4Ragu");
	}

	@Test
	public void extractHtmlString_test() {
		String input = "<p>TEST <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAIAAADOjonJAAABDk\" /></p>";
		String expectedResult = "TEST <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAIAAADOjonJAAABDk\">";
		String result = geminiDataRequestServiceUtils.extractHtmlString(input);
		assertEquals(result, expectedResult);
	}

	@Test
	public void extractHtmlString_multiple_p_tags_test() {
		String input = "<p>What is in the image ? </p><p><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\" /></p>";
		String expectedResult = "What is in the image ? <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">";
		String result = geminiDataRequestServiceUtils.extractHtmlString(input);
		assertEquals(result, expectedResult);
	}

	@Test
	public void extractImgTagsFromArticleContent_test() {
		String input = "What is in the image ? <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">";
		Set<String> expectedResult = Set
				.of("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">");
		Set<String> result = geminiDataRequestServiceUtils.extractImgTagsFromArticleContent(input);
		assertEquals(result, expectedResult);
	}

	@Test
	public void extractImgAttribute_test() {
		String input = "What is in the image ? <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAe\">";

		String result = geminiDataRequestServiceUtils.extractImgAttribute(input);

		assertEquals("iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAe", result);
	}

	@Test
	public void formatRequest_test() {
		String input = "<p>What is in the image ? </p><p><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\" /></p>";
		String expectedResult = """
								{
				  "role": "user",
				  "parts": [
				    {
				      "text": "What is in the image ?"
				    },
				    {
				      "inline_data": {
				        "mime_type": "image/jpeg",
				        "data": "iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;"
				      }
				    }
				  ]
				} """;
		Content result = geminiDataRequestServiceUtils.formatRequest(input);
		assertThat(result).usingRecursiveComparison().ignoringFields("id")
				.isEqualTo(new Gson().fromJson(expectedResult, Content.class));
	}

}
