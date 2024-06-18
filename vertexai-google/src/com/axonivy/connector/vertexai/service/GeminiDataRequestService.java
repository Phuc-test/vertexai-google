package com.axonivy.connector.vertexai.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.connector.vertexai.constants.*;
import com.axonivy.connector.vertexai.entities.*;
import com.axonivy.connector.vertexai.enums.Model;
import com.axonivy.connector.vertexai.enums.Role;
import com.axonivy.connector.vertexai.utils.GeminiDataRequestServiceUtils;

public class GeminiDataRequestService {
	public static final String VERTEX_URL = "https://{0}-aiplatform.googleapis.com/v1/projects/{1}/locations/{0}/publishers/google/models/{2}:generateContent";
	public static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key={0}";
	public static final List<String> vertexAiScopes = List.of("https://www.googleapis.com/auth/cloud-platform");

	public static String VERTEX_PROJECT_ID = Ivy.var().get("vertexai-gemini.projectId");
	public static String VERTEX_LOCATION = Ivy.var().get("vertexai-gemini.location");
	public static String VERTEX_MODEL_NAME = Ivy.var().get("vertexai-gemini.modelName");
	public static String VERTEX_KEY_FILE_PATH = Ivy.var().get("vertexai-gemini.keyFilePath");
	public static String GEMINI_KEY = Ivy.var().get("gemini.apiKey");

	private static List<Content> historyContents = new ArrayList<>();
	private static List<Conversation> conversations = new ArrayList<>();

	private static GeminiDataRequestServiceUtils dataRequestServiceUtils = new GeminiDataRequestServiceUtils();

	public String getAccessToken() throws IOException {
		GoogleCredentials credentials = ServiceAccountCredentials
				.fromStream(GeminiDataRequestServiceUtils.getInputStream(VERTEX_KEY_FILE_PATH))
				.createScoped(vertexAiScopes);
		AccessToken token = credentials.refreshAccessToken();
		return token.getTokenValue();
	}

	public RequestRoot createRequestBody(String message) {
		Content requestContent = dataRequestServiceUtils.formatRequest(message);
		conversations.add(new Conversation(Role.USER.getName(), message));
		historyContents.add(requestContent);
		return new RequestRoot(historyContents);
	}

	public List<Conversation> sendRequestToGemini(String message, Model platFormModel)
			throws IOException, InterruptedException {
		RequestRoot bodyRequestContent = createRequestBody(message);
		// Create HTTP client
		HttpClient client = HttpClient.newHttpClient();
		// Build request
		HttpRequest request = generateHttpRequestBasedOnModel(platFormModel, new Gson().toJson(bodyRequestContent));

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			// Handle response
			Gson gson = new Gson();
			ResponseRoot responseRoot = gson.fromJson(response.body(), ResponseRoot.class);
			Content contentResponse = Optional.ofNullable(responseRoot).map(ResponseRoot::getCandidates)
					.map(Collection::stream).flatMap(Stream::findFirst).map(Candidate::getContent)
					.map(Content::getParts).map(Collection::stream).flatMap(Stream::findFirst).map(Part::getText)
					.map(text -> {
						conversations.add(new Conversation(Role.MODEL.getName(),
								GeminiDataRequestServiceUtils.addCodesToPreTagIfPresent(text)));
						Part currentPart = new Part(text.trim());
						return new Content(Role.MODEL.getName(), List.of(currentPart));
					}).orElse(null);
			historyContents.add(contentResponse);
		} else if (response.statusCode() == 429) {
			Ivy.log().error("Request failed: " + response.statusCode());
			Ivy.log().error(response.body());
			Part currentPart = new Part(Constants.OVERLOADED_SERVER_MESSAGE);
			Content contentResponse = new Content(Role.MODEL.getName(), List.of(currentPart));
			historyContents.add(contentResponse);
			conversations.add(new Conversation(Role.MODEL.getName(), Constants.OVERLOADED_SERVER_MESSAGE));
		} else {
			Ivy.log().error("Request failed: " + response.statusCode());
			Ivy.log().error(response.body());
			historyContents.remove(historyContents.size() - 1);
			conversations.add(new Conversation(Role.MODEL.getName(), Constants.SERVER_ERROR));
		}
		return conversations;
	}

	public void cleanData() {
		historyContents = new ArrayList<>();
		conversations = new ArrayList<>();
	}

	public HttpRequest generateHttpRequestBasedOnModel(Model platformModel, String bodyRequestContent)
			throws IOException {
		if (platformModel == Model.VERTEXAI_GEMINI) {
			String accessToken = getAccessToken();
			String vertexAiGeminiEndpoint = MessageFormat.format(VERTEX_URL, VERTEX_LOCATION, VERTEX_PROJECT_ID,
					VERTEX_MODEL_NAME);
			return HttpRequest.newBuilder().uri(URI.create(vertexAiGeminiEndpoint))
					.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();
		}
		String geminiEndpoint = MessageFormat.format(GEMINI_URL, GEMINI_KEY);
		return HttpRequest.newBuilder().uri(URI.create(geminiEndpoint)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();
	}
}
