package ca.discotek.proxy.vo;

import java.io.Serializable;

public class RequestResponse implements Serializable {

	public final Request request;
	public final Response response;
	
	public RequestResponse(Request request, Response response) {
		this.request = request;
		this.response = response;
	}
	
}
