package com.hortonworks.ranger;

import com.google.common.base.MoreObjects;

public class Input {
	final String host;
	final String repository;
	final String file;
	
	public Input(String hostName, String repositoryName, String fileName) {
		this.host = hostName;
		this.repository = repositoryName;
		this.file = fileName;
	}

	public String toString() {
		return MoreObjects.toStringHelper("Input")
				.add("Host", host)
				.add("RepositoryName", repository)
				.add("FileName", file)
				.toString();
	}
	
	static Input parseInputs(String[] args) {
		String hostName = args[0];
		String repositoryName = args[1];
		String fileName = args[2];
		Input input = new Input(hostName, repositoryName, fileName);
		
		return input;
	}
}
