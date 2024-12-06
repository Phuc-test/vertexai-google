package com.axonivy.connector.vertexai.managedBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.connector.vertexai.entities.*;
import com.axonivy.connector.vertexai.enums.Model;
import com.axonivy.connector.vertexai.enums.Role;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;

@ManagedBean
@ViewScoped
public class GeminiDataBean {
	private String inputtedMessage;
	private Model model;
	private List<Conversation> conversations;
	private GeminiDataRequestService geminiDataRequestService = new GeminiDataRequestService();
	private String errorMessage;
	
	private static final String CODE_RESPONSE_PATTERN = "```(.*?)```";
	private static final String PRE_TAG_PATTERN = "(<pre.*?>.*?</pre>)";
	private static final String EMOJI_PATTERN = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]";
	private static final Set<String> PREFIXES = Set.of("html", "xml", "xhtml");
	private static final String OPEN_ERROR_DIALOG_SCRIPT = "PF('errorDialog').show();";
	private static String PRE_TAG_MAPPING = "<pre style=\"background-color: black;\"> <code>%s</code> </pre>";

	@PostConstruct
	public void init() {
		conversations = new ArrayList<>();
		geminiDataRequestService.cleanData();
	}

	public void onSendRequest() {
		try {
			conversations = geminiDataRequestService.sendRequestToGemini(inputtedMessage, model);
			addCodesToPreTagIfPresent(conversations);
			inputtedMessage = StringUtils.EMPTY;
		} catch (Exception e) {
			errorMessage = e.getMessage();
			PF.current().executeScript(OPEN_ERROR_DIALOG_SCRIPT);
		}
	}

	public void addCodesToPreTagIfPresent(List<Conversation> conversations) {
		Pattern pattern = Pattern.compile(CODE_RESPONSE_PATTERN, Pattern.DOTALL);
		conversations.stream().filter(conversation -> BooleanUtils.isNotTrue(conversation.getIsEntityConverted()))
				.forEach(conversation -> {
					if (Role.MODEL.getName().equals(conversation.getRole())) {
						String result = conversation.getText();
						Matcher matcher = pattern.matcher(conversation.getText());
						List<String> matchedStrings = new ArrayList<>();
						while (matcher.find()) {
							matchedStrings.add(matcher.group(1).trim());
						}
						for (String matchedString : matchedStrings) {
							String convertedString = mapHtmlCodeIfNeeded(matchedString);
							String codeResponse = String.format(PRE_TAG_MAPPING, convertedString);
							result = conversation.getText().replace(matchedString, codeResponse).replaceAll("```",
									StringUtils.EMPTY);

						}
						conversation.setText(escapeExceptPreAndEmoji(result));
						conversation.setIsEntityConverted(true);
					}
				});
	}

	private String mapHtmlCodeIfNeeded(String htmlText) {
		String convertedString = htmlText;
		for (String prefix : PREFIXES) {
			if (htmlText.startsWith(prefix)) {
				convertedString = StringEscapeUtils.escapeHtml(htmlText);
				break;
			}
		}
		return convertedString;
	}

	private String escapeExceptPreAndEmoji(String htmlText) {
		Pattern preTagPattern = Pattern.compile(PRE_TAG_PATTERN, Pattern.DOTALL);
		Matcher matcher = preTagPattern.matcher(htmlText);
		StringBuilder result = new StringBuilder();
		int lastEnd = 0;
		while (matcher.find()) {
			// Append and escape the text before the current <pre> block
			String beforePre = htmlText.substring(lastEnd, matcher.start());
			result.append(escapeExceptEmoji(beforePre));
			// Append the current <pre> block without escaping
			result.append(matcher.group(1));
			lastEnd = matcher.end();
		}
		// Append and escape any remaining text after the last <pre> block
		String afterLastPre = htmlText.substring(lastEnd);
		result.append(escapeExceptEmoji(afterLastPre));

		return result.toString();
	}

	private String escapeExceptEmoji(String text) {
		Pattern emojiPattern = Pattern.compile(EMOJI_PATTERN, Pattern.DOTALL);
		Matcher emojiMatcher = emojiPattern.matcher(text);
		StringBuilder escapedText = new StringBuilder();
		int lastEnd = 0;

		while (emojiMatcher.find()) {
			// Escape the text before the current emoji
			String beforeEmoji = text.substring(lastEnd, emojiMatcher.start());
			String escapeValue = StringEscapeUtils.escapeHtml(beforeEmoji);
			escapedText.append(escapeValue);
			// Append the current emoji without escaping
			escapedText.append(emojiMatcher.group());
			lastEnd = emojiMatcher.end();
		}

		// Escape any remaining text after the last emoji
		String afterLastEmoji = text.substring(lastEnd);
		String escapeValue = StringEscapeUtils.escapeHtml(afterLastEmoji);
		escapedText.append(escapeValue);

		return escapedText.toString();
	}

	public void onCleanText() {
		init();
	}

	public Model[] onSelectModel() {
		return Model.values();
	}

	public String getInputtedMessage() {
		return inputtedMessage;
	}

	public void setInputtedMessage(String inputtedMessage) {
		this.inputtedMessage = inputtedMessage;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public List<Conversation> getConversations() {
		return conversations;
	}

	public void setConversations(List<Conversation> conversations) {
		this.conversations = conversations;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
}
