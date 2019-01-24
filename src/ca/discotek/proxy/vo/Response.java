package ca.discotek.proxy.vo;

import java.io.Serializable;

public class Response implements Serializable {

	public final Message message;
	
	public Response(Message message) {
		this.message = message;
	}
}
