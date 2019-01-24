package ca.discotek.proxy.vo;

import java.io.Serializable;

public class Header implements Serializable {

	public final String text;
	
	public final String name;
	public final String value;
	
	private Header(String text) {
		this.text = text;
		this.name = null;
		this.value = null;
	}
	
	private Header(String name, String value) {
		this.text = null;
		this.name = name;
		this.value = value;
	}
	
	public static Header parseHeader(String header) {
		String chunks[] = header.split(":");
		return chunks.length == 2 ? new Header(chunks[0].trim(), chunks[1].trim()) : new Header(header.trim());
	}
	
	public static Header[] parseHeaders(String headerText) {
		String chunks[] = headerText.split("\n");
		Header headers[] = new Header[chunks.length];
		for (int i=0; i<chunks.length; i++) {
			chunks[i] = chunks[i].trim();
			headers[i] = parseHeader(chunks[i]);
		}
		return headers;
	}
}
