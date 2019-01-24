package ca.discotek.proxy.vo;

import java.io.Serializable;

public class HttpHeader implements Serializable {

    public final String text;
    public final byte bytes[];
    
    public HttpHeader(String text, byte bytes[]) {
        this.text = text;
        this.bytes = bytes;
    }
}
