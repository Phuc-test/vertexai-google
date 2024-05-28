package com.axonivy.managedBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.axonivy.connector.vertexai.entities.*;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;
import com.google.gson.Gson;
import static com.axonivy.connector.vertexai.Constants.jsonContent;
import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class GeminiDataBean {
	private String historyConservation;
	private String inputedMessage;
	private Model model;
	private List<Content> requestContents;
	private GeminiDataRequestService geminiDataRequestService = new GeminiDataRequestService();

	@PostConstruct
	public void init() {
		historyConservation = Strings.EMPTY;
		requestContents = new ArrayList<>();
		geminiDataRequestService.cleanData();
	}

	public void onSendRequest() throws Exception {
		Gson gson = new Gson();
		Ivy.log().warn("inputed message" + inputedMessage);
		historyConservation = geminiDataRequestService.sendRequestToGemini(inputedMessage, "user", model);
		String requestBodyFormat = String.format(jsonContent, historyConservation);
		RequestRoot requestRoot = gson.fromJson(requestBodyFormat, RequestRoot.class);
		requestContents = Optional.ofNullable(requestRoot).map(RequestRoot::getContents).orElse(new ArrayList<>());
		inputedMessage = StringUtils.EMPTY;
	}

	public void onCleanText() {
		init();
		geminiDataRequestService.cleanData();
	}

	public Model[] onSelectModel() {
		return Model.values();
	}

	public String getHistoryConservation() {
		return historyConservation;
	}

	public void setHistoryConservation(String historyConservation) {
		this.historyConservation = historyConservation;
	}

	public List<Content> getRequestContents() {
		return requestContents;
	}

	public void setRequestContents(List<Content> requestContents) {
		this.requestContents = requestContents;
	}

	public String getInputedMessage() {
		return inputedMessage;
	}

	public void setInputedMessage(String inputedMessage) {
		this.inputedMessage = inputedMessage;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

}
