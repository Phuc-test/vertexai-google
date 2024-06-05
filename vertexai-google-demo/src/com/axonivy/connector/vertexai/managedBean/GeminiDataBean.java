package com.axonivy.connector.vertexai.managedBean;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.apache.commons.lang3.StringUtils;
import com.axonivy.connector.vertexai.entities.*;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;

import ch.ivyteam.ivy.environment.Ivy;


@ManagedBean
@ViewScoped
public class GeminiDataBean {
	private String inputtedMessage;
	private Model model;
	private List<Conversation> conversations;
	private GeminiDataRequestService geminiDataRequestService = new GeminiDataRequestService();

	@PostConstruct
	public void init() {
		conversations = new ArrayList<>();
		geminiDataRequestService.cleanData();
	}

	public void onSendRequest() throws Exception {
		Ivy.log().warn(inputtedMessage);
		conversations = geminiDataRequestService.sendRequestToGemini(inputtedMessage, model);
		inputtedMessage = StringUtils.EMPTY;
	}

	public void onCleanText() {
		init();
		geminiDataRequestService.cleanData();
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
