package ca.discotek.proxy.vo;

import java.io.Serializable;

public class HttpBody implements Serializable {

    public final byte rawBytes[];
    public final byte parsedBytes[];
    
    public HttpBody(byte rawBytes[], byte parsedBytes[]) {
        this.rawBytes = rawBytes;
        this.parsedBytes = parsedBytes;
    }
}
