package com.axonivy.connector.vertexai.test.service;

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.axonivy.connector.vertexai.entities.Conversation;
import com.axonivy.connector.vertexai.entities.Model;
import com.axonivy.connector.vertexai.entities.RequestRoot;
import com.axonivy.connector.vertexai.entities.Role;
import com.axonivy.connector.vertexai.mock.DataMock;
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
	MockedStatic<GeminiDataRequestServiceUtils> geminiDataRequestServiceMock;
	MockedStatic<ServiceAccountCredentials> mockedServiceAccountCredentialsStatic;
	MockedStatic<HttpClient> httpClientMockedStatic;

	@BeforeEach
	void beforeEach(AppFixture fixture) {
		fixture.var("vertexai-gemini.projectId", "generate-images-for-process");
		fixture.var("vertexai-gemini.location", "us-central");
		fixture.var("vertexai-gemini.modelName", "gemini-1.5-pro-preview-0409");
		fixture.var("vertexai-gemini.keyFilePath", "D:\\test.json");
		fixture.var("gemini.apiKey", "AIzaSyDaxbn4Ragu");
		geminiDataRequestService = new GeminiDataRequestService();
		geminiDataRequestServiceMock = Mockito.mockStatic(GeminiDataRequestServiceUtils.class);
		mockedServiceAccountCredentialsStatic = Mockito.mockStatic(ServiceAccountCredentials.class);
		httpClientMockedStatic = mockStatic(HttpClient.class);
	}

	@AfterEach
	void afterEach() {
		geminiDataRequestServiceMock.close();
		mockedServiceAccountCredentialsStatic.close();
		httpClientMockedStatic.close();
		geminiDataRequestService.cleanData();
	}

	@Test
	public void createRequestBody_test() {
		String input = "<p>What is in the image ? </p><p><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAAAhCAIAAAAeQ8GBg;\" /></p>";
		String expectedResult = DataMock.load("json/bodyRequestContent.json");
		RequestRoot result = geminiDataRequestService.createRequestBody(input);
		assertThat(result).usingRecursiveComparison().ignoringFields("id")
				.isEqualTo(new Gson().fromJson(expectedResult, RequestRoot.class));
	}

	@Test
	void testGetAccessToken() throws IOException {
		mockAccessToken();
		String actualToken = geminiDataRequestService.getAccessToken();
		// Verify the token value
		assertEquals("mockToken", actualToken);
	}

	@Test
	void testGetAccessTokenThrowsIOException() throws IOException {
		// Stub the behavior of FileInputStream
		geminiDataRequestServiceMock
				.when(() -> GeminiDataRequestServiceUtils.getInputStream(GeminiDataRequestService.VERTEX_KEY_FILE_PATH))
				.thenThrow(new IOException());

		// Call the method under test and expect an IOException
		assertThrows(IOException.class, () -> geminiDataRequestService.getAccessToken());
	}

	@Test
	public void testGenerateHttpRequestBasedOnModel_VertexAI_Gemini() throws IOException {
		// Given
		mockAccessToken();

		Model platformModel = Model.VERTEXAI_GEMINI;
		String bodyRequestContent = DataMock.load("json/bodyRequestContent.json");

		String expectedUrl = MessageFormat.format(GeminiDataRequestService.VERTEX_URL,
				GeminiDataRequestService.VERTEX_LOCATION, GeminiDataRequestService.VERTEX_PROJECT_ID,
				GeminiDataRequestService.VERTEX_MODEL_NAME);
		HttpRequest expectedRequest = HttpRequest.newBuilder().uri(URI.create(expectedUrl))
				.header("Authorization", "Bearer ".concat("mockToken")).header("Content-Type", "application/json")
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

	@Test
	public void testGenerateHttpRequestBasedOnModel_GeminiModel() throws IOException {
		// Given
		Model platformModel = Model.GEMINI;
		String bodyRequestContent = DataMock.load("json/bodyRequestContent.json");

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

	@Test
	public void testSendRequestToGemini_SuccessResponse() throws IOException, InterruptedException {
		// Given
		mockAccessToken();
		String message = "Hello, Gemini!";
		Model platformModel = Model.VERTEXAI_GEMINI;
		String mockResponseBody = DataMock.load("json/responseContent.json");

		// Mocking HttpClient
		HttpClient httpClient = mock(HttpClient.class);
		httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(httpClient);

		// Mocking HttpRequest
		// HttpRequest httpRequest = mock(HttpRequest.class);
		// when(httpRequest.uri()).thenReturn(URI.create("hello gau"));

		// Mocking HttpResponse
		HttpResponse<String> httpResponse = mock(HttpResponse.class);
		when(httpResponse.statusCode()).thenReturn(200);
		when(httpResponse.body()).thenReturn(mockResponseBody);

		httpClientMockedStatic.when(() -> httpClient.send(ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(httpResponse);

		List<Conversation> result = geminiDataRequestService.sendRequestToGemini(message, platformModel);

		// Then
		assertEquals(2, result.size());
		assertEquals("Hello! What can I do for you today?", result.get(1).getText());
		assertEquals(Role.MODEL.getName(), result.get(1).getRole());
	}

	@Test
	public void testSendRequestToGemini_ServerOverLoad() throws IOException, InterruptedException {
		// Given
		mockAccessToken();
		String message = "Hello, Gemini!";
		Model platformModel = Model.VERTEXAI_GEMINI;
		String mockResponseBody = DataMock.load("json/responseContent.json");

		// Mocking HttpClient
		HttpClient httpClient = mock(HttpClient.class);
		httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(httpClient);

		// Mocking HttpRequest
		// HttpRequest httpRequest = mock(HttpRequest.class);
		// when(httpRequest.uri()).thenReturn(URI.create("hello gau"));

		// Mocking HttpResponse
		HttpResponse<String> httpResponse = mock(HttpResponse.class);
		when(httpResponse.statusCode()).thenReturn(429);
		when(httpResponse.body()).thenReturn(mockResponseBody);

		httpClientMockedStatic.when(() -> httpClient.send(ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(httpResponse);

		List<Conversation> result = geminiDataRequestService.sendRequestToGemini(message, platformModel);

		// Then
		assertEquals(2, result.size());
		assertEquals("The server is now overloaded. Please try again later", result.get(1).getText());
		assertEquals(Role.MODEL.getName(), result.get(1).getRole());
	}

	private void mockAccessToken() throws IOException {
		// Given
		String MOCK_TOKEN = "mockToken";

		// Mock the AccessToken
		AccessToken mockAccessToken = new AccessToken(MOCK_TOKEN, null);

		// Mock the GoogleCredentials and ServiceAccountCredentials
		GoogleCredentials mockGoogleCredentials = mock(GoogleCredentials.class);
		ServiceAccountCredentials mockServiceAccountCredentials = mock(ServiceAccountCredentials.class);
		FileInputStream mockFileInputStream = mock(FileInputStream.class);

		// Stub the behavior of FileInputStream
		geminiDataRequestServiceMock
				.when(() -> GeminiDataRequestServiceUtils.getInputStream(GeminiDataRequestService.VERTEX_KEY_FILE_PATH))
				.thenReturn(mockFileInputStream);
		// Use MockedStatic for the static method call to fromStream

		mockedServiceAccountCredentialsStatic.when(() -> ServiceAccountCredentials.fromStream(mockFileInputStream))
				.thenReturn(mockServiceAccountCredentials);

		// When createScoped is called, return the mocked GoogleCredentials
		when(mockServiceAccountCredentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
				.thenReturn(mockGoogleCredentials);

		// When refreshAccessToken is called, return the mocked AccessToken
		when(mockGoogleCredentials.refreshAccessToken()).thenReturn(mockAccessToken);
	}
}