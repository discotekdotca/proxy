package ca.discotek.proxy.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import ca.discotek.proxy.Proxy;
import ca.discotek.proxy.cert.utils.IOUtil;

public class ProxyTest {

    public static void main(String[] args) throws IOException {
        java.net.Proxy proxy = 
            new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(Proxy.DEFAULT_LOCAL_INTERFACE, Proxy.DEFAULT_LOCAL_PORT));
        
//        URL url = new URL("https://www.google.ca/?q=asdf");
//        URL url = new URL("https://www.google.ca/");
//        URL url = new URL("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png");
        URL url = new URL("http://www.tutorialspoint.com/");
        
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection c = (HttpURLConnection) url.openConnection(proxy);
        
        
        InputStream is = c.getInputStream();
        IOUtil.copy(is, System.out);
    }
}
