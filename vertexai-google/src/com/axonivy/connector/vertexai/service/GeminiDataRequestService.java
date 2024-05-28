package com.axonivy.connector.vertexai.service;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.apache.commons.lang3.StringUtils;
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

import static com.axonivy.connector.vertexai.Constants.*;

public class GeminiDataRequestService {

	private static final String keyFilePath = "D:\\generate-images-for-process-27dba695f5f5.json";
	private static final List<String> vertexAiScopes = List.of("https://www.googleapis.com/auth/cloud-platform");
	private static final String PROJECT_ID = "generate-images-for-process";
	private static final String LOCATION = "us-central1";
	private static final String modelName = "gemini-1.5-pro-preview-0409";
	public static final String IMG_TAG_PATTERN = "<img\\s+[^>]*>";
	public static final String IMG_SRC_ATTR_PATTERN = "data:image\\/[^;]+;base64,([^\"]+)";

	private static String historyContent = "";
	private static final String ENDPOINT = "https://us-central1-aiplatform.googleapis.com/v1/projects/generate-images-for-process/locations/us-central1/publishers/google/models/gemini-1.5-pro-preview-0409:generateContent";

	public static String getAccessToken() throws Exception {
		GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(keyFilePath))
				.createScoped(vertexAiScopes);
		AccessToken token = credentials.refreshAccessToken();
		return token.getTokenValue();
	}

	private static String formatRequest(String message, String role) {
        String content = extractHtmlString(message);
        List<String> imgTags = extractImgTagsFromArticleContent(content).stream().toList();

        if (ObjectUtils.isNotEmpty(imgTags)) {
            String imageObjects = Strings.EMPTY;
            for (String imgTag : imgTags) {
                String imageBase64 = extractImgAttribute(imgTag);
                String mapToImageFormat = String.format(imageInput, imageBase64);
                imageObjects = StringUtils.isEmpty(imageObjects) ? mapToImageFormat
                        : String.join(",", imageObjects, imageObjects);
                content = content.replace(imgTag, "");
            }
            return String.format(textAndImageInput, role, content.trim(), imageObjects);
        }
        return String.format(onlyTextInput, role, content);
	}

	private String createRequestBody(String message, String role) {
		String requestContent = formatRequest(message, role);
		historyContent = StringUtils.isEmpty(historyContent) ? requestContent
				: String.join(",", historyContent, requestContent);

		return String.format(jsonContent, historyContent);
	}

	public String sendRequestToGemini(String message, String role) throws Exception {
		String bodyRequestContent = createRequestBody(message, role);
		Ivy.log().warn("bodyRequestContent ne " + bodyRequestContent);
		String accessToken = getAccessToken();
		// Create HTTP client
		HttpClient client = HttpClient.newHttpClient();
		// Build request
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ENDPOINT))
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(bodyRequestContent)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			// Handle response
			Gson gson = new Gson();
			ResponseRoot responseRoot = gson.fromJson(response.body(), ResponseRoot.class);
			String multiTurnResponse = Optional.ofNullable(responseRoot).map(ResponseRoot::getCandidates)
					.map(Collection::stream).flatMap(Stream::findFirst).map(Candidate::getContent)
					.map(Content::getParts).map(Collection::stream).flatMap(Stream::findFirst).map(Part::getText)
					.map(gson::toJson).map(text -> String.format(onlyTextOutput, "model", text)).orElse("");
			historyContent = String.join(COMMA, historyContent, multiTurnResponse);
		} else {
			Ivy.log().error("Request failed: " + response.statusCode());
			Ivy.log().error(response.body());
		}
		return historyContent;
	}

	public void cleanData() {
		historyContent = Strings.EMPTY;
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
}
