package com.axonivy.connector.vertexai.entities;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResponseRoot {
	@SerializedName("candidates")
	private List<Candidate> candidates;

	public List<Candidate> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<Candidate> candidates) {
		this.candidates = candidates;
	}
}
