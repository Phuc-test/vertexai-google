package com.axonivy.connector.vertexai.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.axonivy.connector.vertexai.constants.Constants;
import com.axonivy.connector.vertexai.entities.Content;
import com.axonivy.connector.vertexai.entities.InlineData;
import com.axonivy.connector.vertexai.entities.Part;
import com.axonivy.connector.vertexai.enums.Role;

public class GeminiDataRequestServiceUtils {
	public static final String IMG_TAG_PATTERN = "<img\\s+[^>]*>";
	public static final String IMG_SRC_ATTR_PATTERN = "data:image\\/[^;]+;base64,([^\"]+)";

	public static InputStream getInputStream(String keyFilePath) throws IOException {
		return new FileInputStream(keyFilePath);
	}

	public Content formatRequest(String message) {
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

	public Set<String> extractImgTagsFromArticleContent(String content) {
		Set<String> imgTags = new HashSet<>();
		Pattern pattern = Pattern.compile(IMG_TAG_PATTERN);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			var foundImgTag = matcher.group();
			imgTags.add(foundImgTag);
		}
		return imgTags;
	}

	public String extractImgAttribute(String imgTag) {
		Pattern pattern = Pattern.compile(IMG_SRC_ATTR_PATTERN);
		Matcher matcher = pattern.matcher(imgTag);
		String imgAttribute = Strings.EMPTY;
		while (matcher.find()) {
			imgAttribute = matcher.group(1);
		}
		return imgAttribute;
	}

	public String extractHtmlString(String htmlContent) {
		Document doc = Jsoup.parse(htmlContent);
		Elements content = doc.select("p");
		return content.stream().map(Element::html).filter(html -> !Constants.BR_TAG.equals(html))
				.collect(Collectors.joining(StringUtils.SPACE));
	}

}
