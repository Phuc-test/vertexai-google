package com.axonivy.managedBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FilesUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

import com.axonivy.connector.vertexai.entities.*;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService;
import com.axonivy.connector.vertexai.service.GeminiDataRequestService2;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class GeminiDataBean2 {
	private String inputtedMessage;
	private Model model;
	private List<Conversation> conversations;
	private GeminiDataRequestService2 geminiDataRequestService2 = new GeminiDataRequestService2();

	private UploadedFiles files;
	private List<String> base64Images;

	@PostConstruct
	public void init() {
		base64Images = new ArrayList<>();
		conversations = new ArrayList<>();
		geminiDataRequestService2.cleanData();
	}

	public void onSendRequest() throws Exception {
		handleFilesUpload();
		conversations = geminiDataRequestService2.sendRequestToGemini(inputtedMessage, base64Images, model);
		inputtedMessage = StringUtils.EMPTY;
	}

	public void onCleanText() {
		init();
		geminiDataRequestService2.cleanData();
		base64Images = new ArrayList<>();
	}

	public void handleFilesUpload() {
		if (files != null) {
			for (UploadedFile file : files.getFiles()) {
				try (InputStream input = file.getInputStream()) {
					base64Images.add(java.util.Base64.getEncoder().encodeToString(input.readAllBytes()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
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

	public UploadedFiles getFiles() {
		return files;
	}

	public void setFiles(UploadedFiles files) {
		this.files = files;
	}

	public List<String> getBase64Images() {
		return base64Images;
	}

	public void setBase64Images(List<String> base64Images) {
		this.base64Images = base64Images;
	}
}
