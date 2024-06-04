package com.axon.market.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.axonivy.connector.vertexai.entities.Content;
import com.axonivy.connector.vertexai.entities.RequestRoot;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;

@IvyProcessTest
public class GeminiDataRequestServiceTest {
	private GeminiDataRequestService geminiDataRequestService =  new GeminiDataRequestService();;
	
	@BeforeEach
	void beforeEach(AppFixture fixture) {
		fixture.var("vertexai-gemini.projectId", "generate-images-for-process");
		fixture.var("vertexai-gemini.location", "us-central");
		fixture.var("vertexai-gemini.modelName", "gemini-1.5-pro-preview-0409");
		fixture.var("vertexai-gemini.keyFilePath", "D:\\test.json");
//		geminiDataRequestService = new GeminiDataRequestService();
	}
	
	

	@Test
	public void extractHtmlString_test() {
		String input = "<p>TEST <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAIAAADOjonJAAABDk\" /></p>";
		String expectedResult = "TEST <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAIAAADOjonJAAABDk\">";
		String result = geminiDataRequestService.extractHtmlString(input);
		assertEquals(result, expectedResult);
	}

	@Test
	public void extractHtmlString_multiple_p_tags_test() {
		String input = "<p>What is in the image ? </p><p><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\" /></p>";
		String expectedResult = "What is in the image ? <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">";
		String result = geminiDataRequestService.extractHtmlString(input);
		assertEquals(result, expectedResult);
	}

	@Test
	public void extractImgTagsFromArticleContent_test() {
		String input = "What is in the image ? <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">";
		Set<String> expectedResult = Set
				.of("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\">");
		Set<String> result = geminiDataRequestService.extractImgTagsFromArticleContent(input);
		assertEquals(result, expectedResult);
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
		Content result = geminiDataRequestService.formatRequest(input);
		assertThat(result).usingRecursiveComparison().ignoringFields("id")
				.isEqualTo(new Gson().fromJson(expectedResult, Content.class));
	}

	@Test
	public void createRequestBody_test() {
		String input = "<p>What is in the image ? </p><p><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\" /></p>";
		String expectedResult = """
					{
					  "contents": [
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
					    }
					  ]
					}
				""";
		RequestRoot result = geminiDataRequestService.createRequestBody(input);
		assertThat(result).usingRecursiveComparison().ignoringFields("id")
				.isEqualTo(new Gson().fromJson(expectedResult, RequestRoot.class));
	}

	@Test
	void testGetAccessToken() throws IOException {
		// Mock the FileInputStream
		String MOCK_TOKEN = "mockToken";

		// Mock the AccessToken
		AccessToken mockAccessToken = new AccessToken(MOCK_TOKEN, null);

		// Mock the GoogleCredentials and ServiceAccountCredentials
		GoogleCredentials mockGoogleCredentials = mock(GoogleCredentials.class);
		ServiceAccountCredentials mockServiceAccountCredentials = mock(ServiceAccountCredentials.class);

		// Use MockedStatic for the static method call to fromStream
		try (MockedStatic<ServiceAccountCredentials> mockedServiceAccountCredentialsStatic = mockStatic(
				ServiceAccountCredentials.class)) {

			mockedServiceAccountCredentialsStatic
					.when(() -> ServiceAccountCredentials.fromStream(any(FileInputStream.class)))
					.thenReturn(mockServiceAccountCredentials);

			// When createScoped is called, return the mocked GoogleCredentials
			when(mockServiceAccountCredentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
					.thenReturn(mockGoogleCredentials);
			// When refreshAccessToken is called, return the mocked AccessToken
			when(mockGoogleCredentials.refreshAccessToken()).thenReturn(mockAccessToken);

			// Call the method under test
			String actualToken = geminiDataRequestService.getAccessToken();

			// Verify the token value
			assertEquals(MOCK_TOKEN, actualToken);
			// Verify interactions
			verify(mockServiceAccountCredentials)
					.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
			verify(mockGoogleCredentials).refreshAccessToken();
		}
	}

	@Test
	void testGetAccessTokenThrowsIOException() throws IOException {
		
		assertThrows(IOException.class, geminiDataRequestService::getAccessToken);
	}
}
