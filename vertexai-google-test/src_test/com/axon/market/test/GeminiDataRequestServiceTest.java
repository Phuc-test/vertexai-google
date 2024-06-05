package com.axon.market.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.text.MessageFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.axonivy.connector.vertexai.entities.Model;
import com.axonivy.connector.vertexai.entities.RequestRoot;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;
import com.axonivy.connector.vertexai.utils.GeminiDataRequestServiceUtils;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;

@IvyProcessTest
public class GeminiDataRequestServiceTest {
	private GeminiDataRequestService geminiDataRequestService;

	@BeforeEach
	void beforeEach(AppFixture fixture) {
		fixture.var("vertexai-gemini.projectId", "generate-images-for-process");
		fixture.var("vertexai-gemini.location", "us-central");
		fixture.var("vertexai-gemini.modelName", "gemini-1.5-pro-preview-0409");
		fixture.var("vertexai-gemini.keyFilePath", "D:\\test.json");
		fixture.var("gemini.apiKey", "AIzaSyDaxbn4Ragu");
		geminiDataRequestService = new GeminiDataRequestService();
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

		FileInputStream mockFileInputStream = mock(FileInputStream.class);

		try (MockedStatic<GeminiDataRequestServiceUtils> geminiDataRequestServiceMock = Mockito
				.mockStatic(GeminiDataRequestServiceUtils.class)) {
			// Stub the behavior of FileInputStream
			geminiDataRequestServiceMock.when(
					() -> GeminiDataRequestServiceUtils.getInputStream(GeminiDataRequestService.VERTEX_KEY_FILE_PATH))
					.thenReturn(mockFileInputStream);
			// Use MockedStatic for the static method call to fromStream
			try (MockedStatic<ServiceAccountCredentials> mockedServiceAccountCredentialsStatic = mockStatic(
					ServiceAccountCredentials.class)) {

				mockedServiceAccountCredentialsStatic
						.when(() -> ServiceAccountCredentials.fromStream(any(FileInputStream.class)))
						.thenReturn(mockServiceAccountCredentials);

				// When createScoped is called, return the mocked GoogleCredentials
				when(mockServiceAccountCredentials
						.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
						.thenReturn(mockGoogleCredentials);
				// When refreshAccessToken is called, return the mocked AccessToken
				when(mockGoogleCredentials.refreshAccessToken()).thenReturn(mockAccessToken);

				String actualToken = geminiDataRequestService.getAccessToken();

				// Verify the token value
				assertEquals(MOCK_TOKEN, actualToken);
				// Verify interactions
				verify(mockServiceAccountCredentials)
						.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
				verify(mockGoogleCredentials).refreshAccessToken();
			}

		}
	}

	@Test
	void testGetAccessTokenThrowsIOException() throws IOException {
		try (MockedStatic<GeminiDataRequestServiceUtils> serviceUtilsMockedStatic = Mockito
				.mockStatic(GeminiDataRequestServiceUtils.class)) {
			// Stub the behavior of FileInputStream
			serviceUtilsMockedStatic.when(
					() -> GeminiDataRequestServiceUtils.getInputStream(GeminiDataRequestService.VERTEX_KEY_FILE_PATH))
					.thenThrow(new IOException());

			// Call the method under test and expect an IOException
			assertThrows(IOException.class, () -> geminiDataRequestService.getAccessToken());
		}
	}

	@Test
	public void testGenerateHttpRequestBasedOnModel_VertexAI_Gemini() throws IOException {
		// Given
		String MOCK_TOKEN = "mockToken";

		// Mock the AccessToken
		AccessToken mockAccessToken = new AccessToken(MOCK_TOKEN, null);

		// Mock the GoogleCredentials and ServiceAccountCredentials
		GoogleCredentials mockGoogleCredentials = mock(GoogleCredentials.class);
		ServiceAccountCredentials mockServiceAccountCredentials = mock(ServiceAccountCredentials.class);
		FileInputStream mockFileInputStream = mock(FileInputStream.class);
		try (MockedStatic<GeminiDataRequestServiceUtils> geminiDataRequestServiceMock = Mockito
				.mockStatic(GeminiDataRequestServiceUtils.class)) {
			// Stub the behavior of FileInputStream
			geminiDataRequestServiceMock.when(
					() -> GeminiDataRequestServiceUtils.getInputStream(GeminiDataRequestService.VERTEX_KEY_FILE_PATH))
					.thenReturn(mockFileInputStream);
			// Use MockedStatic for the static method call to fromStream
			try (MockedStatic<ServiceAccountCredentials> mockedServiceAccountCredentialsStatic = mockStatic(
					ServiceAccountCredentials.class)) {
				mockedServiceAccountCredentialsStatic
						.when(() -> ServiceAccountCredentials.fromStream(any(FileInputStream.class)))
						.thenReturn(mockServiceAccountCredentials);

				// When createScoped is called, return the mocked GoogleCredentials
				when(mockServiceAccountCredentials
						.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
						.thenReturn(mockGoogleCredentials);

				// When refreshAccessToken is called, return the mocked AccessToken
				when(mockGoogleCredentials.refreshAccessToken()).thenReturn(mockAccessToken);

				Model platformModel = Model.VERTEXAI_GEMINI;
				String bodyRequestContent = """
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

				String expectedUrl = MessageFormat.format(GeminiDataRequestService.VERTEX_URL,
						GeminiDataRequestService.VERTEX_LOCATION, GeminiDataRequestService.VERTEX_PROJECT_ID,
						GeminiDataRequestService.VERTEX_MODEL_NAME);
				HttpRequest expectedRequest = HttpRequest.newBuilder().uri(URI.create(expectedUrl))
						.header("Authorization", "Bearer ".concat(MOCK_TOKEN))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();

				// When
				HttpRequest actualRequest = geminiDataRequestService.generateHttpRequestBasedOnModel(platformModel,
						bodyRequestContent);

				// Then
				assertEquals(expectedRequest.uri(), actualRequest.uri());
				assertEquals(expectedRequest.headers().map(), actualRequest.headers().map());
				assertEquals(expectedRequest.bodyPublisher().get().contentLength(),
						actualRequest.bodyPublisher().get().contentLength());
			}

		}

	}

	@Test
	public void testGenerateHttpRequestBasedOnModel_GeminiModel() throws IOException {
		// Given
		Model platformModel = Model.GEMINI;
		String bodyRequestContent = """
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

		String expectedUrl = MessageFormat.format(GeminiDataRequestService.GEMINI_URL,
				GeminiDataRequestService.GEMINI_KEY);
		HttpRequest expectedRequest = HttpRequest.newBuilder().uri(URI.create(expectedUrl))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();

		// When
		HttpRequest actualRequest = geminiDataRequestService.generateHttpRequestBasedOnModel(platformModel,
				bodyRequestContent);

		// Then
		assertEquals(expectedRequest.uri(), actualRequest.uri());
		assertEquals(expectedRequest.headers().map(), actualRequest.headers().map());
		assertEquals(expectedRequest.bodyPublisher().get().contentLength(),
				actualRequest.bodyPublisher().get().contentLength());
	}
}
