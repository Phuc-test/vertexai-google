package com.axonivy.connector.vertexai.service;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.connector.vertexai.entities.*;

public class GeminiDataRequestService {

	private static final List<String> vertexAiScopes = List.of("https://www.googleapis.com/auth/cloud-platform");
	private static final String VERTEX_PROJECT_ID = Ivy.var().get("vertexai-gemini.projectId");
	private static final String VERTEX_LOCATION = Ivy.var().get("vertexai-gemini.location");
	private static final String VERTEX_MODEL_NAME = Ivy.var().get("vertexai-gemini.modelName");
	private static final String VERTEX_KEY_FILE_PATH = Ivy.var().get("vertexai-gemini.keyFilePath");
	private static final String GEMINI_KEY = Ivy.var().get("gemini.apiKey");

	public static final String IMG_TAG_PATTERN = "<img\\s+[^>]*>";
	public static final String IMG_SRC_ATTR_PATTERN = "data:image\\/[^;]+;base64,([^\"]+)";

	private static List<Content> historyContent = new ArrayList<>();
	private static List<Conversation> conversations = new ArrayList<>();
	private static final String VERTEX_URL = "https://{0}-aiplatform.googleapis.com/v1/projects/{1}/locations/{0}/publishers/google/models/{2}:generateContent";
	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key={0}";

	public static String getAccessToken() throws Exception {
		GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(VERTEX_KEY_FILE_PATH))
				.createScoped(vertexAiScopes);
		AccessToken token = credentials.refreshAccessToken();
		return token.getTokenValue();
	}

	private static Content formatRequest(String message) {
		String content = extractHtmlString(message);
		List<String> imgTags = extractImgTagsFromArticleContent(content).stream().toList();
		if (ObjectUtils.isNotEmpty(imgTags)) {
			List<Part> parts = new ArrayList<>();
			for (String imgTag : imgTags) {
				content = content.replace(imgTag, Strings.EMPTY);
				String imageBase64 = extractImgAttribute(imgTag);
				InlineData inlineData = new InlineData("image/jpeg", imageBase64);
				Part currentPart = new Part(inlineData);
				parts.add(currentPart);
			}
			parts.add(0, new Part(content.trim()));
			return new Content(Role.USER.getName(), parts);
		}
		Part currentPart = new Part(content.trim());
		return new Content(Role.USER.getName(), List.of(currentPart));
	}

	private static RequestRoot createRequestBody(String message) {
		Content requestContent = formatRequest(message);
		conversations.add(new Conversation(Role.USER.getName(), message));
		historyContent.add(requestContent);
		return new RequestRoot(historyContent);
	}

	public List<Conversation> sendRequestToGemini(String message, Model platFormModel) throws Exception {
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
						conversations.add(new Conversation(Role.MODEL.getName(), text.trim()));
						Part currentPart = new Part(text.trim());
						return new Content(Role.MODEL.getName(), List.of(currentPart));
					}).orElse(null);
			historyContent.add(contentResponse);
		} else if (response.statusCode() == 429) {
			Ivy.log().error("Request failed: " + response.statusCode());
			Ivy.log().error(response.body());
			Part currentPart = new Part("The server is now overloaded. Please try again later");
			Content contentResponse = new Content(Role.MODEL.getName(), List.of(currentPart));
			historyContent.add(contentResponse);
			conversations.add(
					new Conversation(Role.MODEL.getName(), "The server is now overloaded. Please try again later"));
		} else {
			Ivy.log().error("Request failed: " + response.statusCode());
			Ivy.log().error(response.body());
			Part currentPart = new Part("There are some issue in server. Please try again later");
			Content contentResponse = new Content(Role.MODEL.getName(), List.of(currentPart));
			historyContent.add(contentResponse);
			conversations.add(
					new Conversation(Role.MODEL.getName(), "There are some issue in server. Please try again later"));
		}
		return conversations;
	}

	public void cleanData() {
		historyContent = new ArrayList<>();
		conversations = new ArrayList<>();
	}

	private static Set<String> extractImgTagsFromArticleContent(String content) {
		Set<String> imgTags = new HashSet<>();
		Pattern pattern = Pattern.compile(IMG_TAG_PATTERN);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			var foundImgTag = matcher.group();
			imgTags.add(foundImgTag);
		}
		return imgTags;
	}

	private static String extractImgAttribute(String imgTag) {
		Pattern pattern = Pattern.compile(IMG_SRC_ATTR_PATTERN);
		Matcher matcher = pattern.matcher(imgTag);
		String imgAttribute = Strings.EMPTY;
		while (matcher.find()) {
			imgAttribute = matcher.group(1);
		}
		return imgAttribute;
	}

	private static String extractHtmlString(String htmlContent) {
		Document doc = Jsoup.parse(htmlContent);
		Elements content = doc.select("p");
		return content.stream().map(Element::html).collect(Collectors.joining(" "));
	}

	private HttpRequest generateHttpRequestBasedOnModel(Model platformModel, String bodyRequestContent)
			throws Exception {
		if (platformModel == Model.VERTEXAI_GEMINI) {
			String accessToken = getAccessToken();
			String VERTEXAI_GEMINI_ENDPOINT = MessageFormat.format(VERTEX_URL, VERTEX_LOCATION, VERTEX_PROJECT_ID,
					VERTEX_MODEL_NAME);
			return HttpRequest.newBuilder().uri(URI.create(VERTEXAI_GEMINI_ENDPOINT))
					.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();
		}
		String GEMINI_ENDPOINT = MessageFormat.format(GEMINI_URL, GEMINI_KEY);
		return HttpRequest.newBuilder().uri(URI.create(GEMINI_ENDPOINT)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();
	}
}
