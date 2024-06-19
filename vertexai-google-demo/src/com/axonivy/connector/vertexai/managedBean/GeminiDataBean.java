package com.axonivy.connector.vertexai.managedBean;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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

	private static final String CODE_RESPONSE_PATTERN = "```(.*?)```";
	private static final String PRE_TAG_PATTERN = "(<pre.*?>.*?</pre>)";

	@PostConstruct
	public void init() {
		conversations = new ArrayList<>();
		geminiDataRequestService.cleanData();
	}

	public void onSendRequest() throws Exception {
		conversations = geminiDataRequestService.sendRequestToGemini(inputtedMessage, model);
		addCodesToPreTagIfPresent(conversations);
		inputtedMessage = StringUtils.EMPTY;
	}

	public void addCodesToPreTagIfPresent(List<Conversation> conversations) {
		Pattern pattern = Pattern.compile(CODE_RESPONSE_PATTERN, Pattern.DOTALL);
		conversations.forEach(conversation -> {
			if (conversation.getRole().equals(Role.MODEL.getName())) {
				String result = conversation.getText();
				Matcher matcher = pattern.matcher(conversation.getText());
				List<String> matchedStrings = new ArrayList<>();
				while (matcher.find()) {
					matchedStrings.add(matcher.group(1).trim());
				}
				for (String matchedString : matchedStrings) {
					String convertedString = matchedString;
					if (matchedString.startsWith("html") || matchedString.startsWith("xml")
							|| matchedString.startsWith("xhtml")) {
						convertedString = StringEscapeUtils.escapeHtml(matchedString);
					}
					String codeResponse = String
							.format("<pre style=\"background-color: black;\"> <code>%s</code> </pre>", convertedString);

					result = conversation.getText().replace(matchedString, codeResponse).replaceAll("```", "");

				}
				conversation.setText(escapeExceptPre(result));
			}
		});
	}

	private String escapeExceptPre(String htmlText) {
		Pattern preTagPattern = Pattern.compile(PRE_TAG_PATTERN, Pattern.DOTALL);
		Matcher matcher = preTagPattern.matcher(htmlText);
		StringBuffer result = new StringBuffer();
		// Index to keep track of the last match's end
		int lastEnd = 0;
		while (matcher.find()) {
			// Append and escape the text before the current <pre> block
			String beforePre = htmlText.substring(lastEnd, matcher.start());
			result.append(StringEscapeUtils.escapeHtml(beforePre));
			// Append the current <pre> block without escaping
			result.append(matcher.group(1));
			// Update the last match's end index
			lastEnd = matcher.end();
		}
		// Append and escape any remaining text after the last <pre> block
		String afterLastPre = htmlText.substring(lastEnd);
		result.append(StringEscapeUtils.escapeHtml(afterLastPre));
		return result.toString();
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
}
