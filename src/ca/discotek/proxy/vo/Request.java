package ca.discotek.proxy.vo;

import java.io.Serializable;

public class Request implements Serializable {

	public final Message message;
	public final String method;
	public final String host;
	public final String url;
	
	public Request(Message message) {
		this.message = message;
		String localMethod = null;
		String localHost = null;
		String localUrl = null;
		
		for (int i=0; i<message.headers.length; i++) {
			if (message.headers[i].text != null && i == 0) {
				String chunks[] = message.headers[0].text.split("\\s");
				localMethod = chunks[0];
				localUrl = chunks[1];
			}
			else if (message.headers[i].name != null && message.headers[i].name.equalsIgnoreCase("host"))
				localHost = message.headers[i].value;
		}
		
		this.method = localMethod;
		this.host = localHost;
		this.url = localUrl;
	}
}
