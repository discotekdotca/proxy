package ca.discotek.proxy.cert.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte bytes[] = new byte[1024];
        int length;
        
        while ( (length = is.read(bytes)) > 0)
            os.write(bytes, 0, length);
	}
}
