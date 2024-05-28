package com.axonivy.managedBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	private List<Content> requestContents;
	private GeminiDataRequestService geminiDataRequestService = new GeminiDataRequestService();

	@PostConstruct
	public void init() {
		requestContents = new ArrayList<>();
		geminiDataRequestService.cleanData();
	}

	public void onSendRequest() throws Exception {
		Ivy.log().warn(inputtedMessage);
		RequestRoot requestRoot = geminiDataRequestService.sendRequestToGemini(inputtedMessage, model);
		requestContents = Optional.ofNullable(requestRoot).map(RequestRoot::getContents).orElse(new ArrayList<>());
		inputtedMessage = StringUtils.EMPTY;
	}

	public void onCleanText() {
		init();
		geminiDataRequestService.cleanData();
	}

	public Model[] onSelectModel() {
		return Model.values();
	}

	public List<Content> getRequestContents() {
		return requestContents;
	}

	public void setRequestContents(List<Content> requestContents) {
		this.requestContents = requestContents;
	}

	public String getInputedMessage() {
		return inputtedMessage;
	}

	public void setInputedMessage(String inputedMessage) {
		this.inputtedMessage = inputedMessage;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

}
