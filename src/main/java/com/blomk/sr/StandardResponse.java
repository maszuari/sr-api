package com.blomk.sr;

import java.util.Set;

public class StandardResponse {

	private StatusResponse status;
	private String message;
	private Set<String> urls;
	
	public StandardResponse(StatusResponse status) {
        this.status = status;
    }
    public StandardResponse(StatusResponse status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public StandardResponse(StatusResponse status, String message, Set<String> urls) {
        this.status = status;
        this.message = message;
        this.urls = urls;
     
    }
    
	public StatusResponse getStatus() {
		return status;
	}
	public void setStatus(StatusResponse status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Set<String> getFilenames() {
		return urls;
	}
	public void setFilenames(Set<String> filenames) {
		this.urls = filenames;
	}
}
