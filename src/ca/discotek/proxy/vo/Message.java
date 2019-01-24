package ca.discotek.proxy.vo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Message implements Serializable {

    static final byte EMPTY_BYTES[] = new byte[0];
    
	public HttpHeader httpHeader;
	public Header headers[];
	public HttpBody body;
	
	public Message(HttpHeader httpHeader, Header headers[], HttpBody body) {
		this.httpHeader = httpHeader;
		this.headers = headers;
		this.body = body;
	}
	
	public static Message parseMessage(InputStream is, boolean isRequest) throws IOException {
		HttpHeader httpHeader = readHeader(is);
		
		boolean isChunked = false;
		Integer contentLength = null;
		Header headers[] = Header.parseHeaders(httpHeader.text);
		for (int i=0; i<headers.length; i++) {
			if (headers[i].name != null && headers[i].name.equalsIgnoreCase("content-length"))
				contentLength = Integer.parseInt(headers[i].value);
			else if (headers[i].name != null && headers[i].name.equalsIgnoreCase("transfer-encoding") && headers[i].value.equalsIgnoreCase("chunked"))
			    isChunked = true;
		}
		
		HttpBody body;
		
		if (isRequest) {
            if (headers.length > 0) {
                if (headers[0].text != null && headers[0].text.toLowerCase().startsWith("get"))
                    body = new HttpBody(EMPTY_BYTES, EMPTY_BYTES);
                else if (headers[0].name != null && headers[0].name.toLowerCase().startsWith("get"))
                    body = new HttpBody(EMPTY_BYTES, EMPTY_BYTES);
                else
                    body = readBody(is, contentLength, isChunked);
            }
            else
                body = readBody(is, contentLength, isChunked);
		}
		else {
		    String chunks[] = headers[0].text.split("\\s");
		    Integer responseCode = Integer.parseInt(chunks[1]);
		    
		    if (responseCode == 200)
		        body = readBody(is, contentLength, isChunked);
		    else
		        body = new HttpBody(EMPTY_BYTES, EMPTY_BYTES);
		}
		
		
		return new Message(httpHeader, headers, body);
	}

	public static synchronized HttpHeader readHeader(InputStream is) throws IOException {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    
		String msg = "";
        int		oneByte = -1;
        boolean eoh = false;
        StringBuilder sb = new StringBuilder(200);
        
        do {
            oneByte = is.read();
            bos.write(oneByte);
        	
        	if (oneByte == -1) {
        		eoh = true;
				break;
        	}
            sb.append((char) oneByte);

            if (((char) oneByte) == '\n' && isHeaderEnd(sb)) {
                eoh = true;
                msg = sb.toString();
            }
		} while (!eoh);
        
        
        
        return new HttpHeader(msg, bos.toByteArray());

	}
	
	private static final String CRLF = "\r\n";
	private static final String CRLF2 = CRLF + CRLF;
	private static final String LF = "\n";
	private static final String LF2 = LF + LF;
	
	private static final boolean isHeaderEnd(StringBuilder sb) {
		int len = sb.length();
		if (len > 2) {
			if (LF2.equals(sb.substring(len-2))) {
				return true;
			}
		}
	
		if (len > 4) {
			if (CRLF2.equals(sb.substring(len-4))) {
				return true;
			}
		}
	
		return false;
	}

	static void readBytes(int count, InputStream is, OutputStream os) throws IOException {
	    for (int i=0; i<count; i++)
	        os.write(is.read());
	}
	
	public static HttpBody readBody(InputStream is, Integer contentLength, boolean isChunked) throws IOException {
	    
	    if (isChunked) {
	        ByteArrayOutputStream rawBos = new ByteArrayOutputStream();
	        ByteArrayOutputStream parsedBytesBos = new ByteArrayOutputStream();
	        
	        int chunkLength = -1;
	        
	        byte buffer[] = new byte[1024];
	        int chunkRead, length;
	        
            StringBuilder hexBuffer = new StringBuilder();
            int value;
            char previousChar;
            char currentChar;
	        while (true) {
	            previousChar = '\0';
	            currentChar = '\0';
	            hexBuffer.setLength(0);

                while (previousChar != '\r' && currentChar != '\n') {
	                value = is.read();
	                previousChar = currentChar;
	                currentChar = (char) value;
	                rawBos.write(value);
	                if (currentChar != '\r' && currentChar != '\n')
	                    hexBuffer.append( (char) value );
	            }
	            
	            chunkLength = Integer.parseInt(hexBuffer.toString(), 16);
	            if (chunkLength == 0)
	                break;

	            chunkRead = 0;
	            while (chunkRead < chunkLength) {
	                if (chunkLength - chunkRead < buffer.length)
	                    buffer = new byte[chunkLength - chunkRead];
	                length = is.read(buffer);
	                parsedBytesBos.write(buffer, 0, length);
	                rawBos.write(buffer, 0, length);
	                chunkRead += length;
	            }
	            
	            // read CRLF
                readBytes(2, is, rawBos);
	        }
	        
	        return new HttpBody(rawBos.toByteArray(), parsedBytesBos.toByteArray());
	    }
	    else {
	        int totalRead = 0;
	        ByteArrayOutputStream rawBos = new ByteArrayOutputStream();
	        int length;
	        byte buffer[] = new byte[1024];
//	        while ( (length = is.read(buffer)) > 0) {
	        while ( totalRead < contentLength ) {
	            length = is.read(buffer);
	            rawBos.write(buffer, 0, length);
	            totalRead += length;
	        }
	        
	        return new HttpBody(rawBos.toByteArray(), rawBos.toByteArray());
	    }
	}
}
