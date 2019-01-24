package ca.discotek.proxy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JButton;
import javax.swing.JFrame;

import ca.discotek.proxy.cert.utils.CertUtil;
import ca.discotek.proxy.cert.utils.IOUtil;
import ca.discotek.proxy.data.DataManager;
import ca.discotek.proxy.vo.Message;
import ca.discotek.proxy.vo.Request;
import ca.discotek.proxy.vo.RequestResponse;
import ca.discotek.proxy.vo.Response;

public class Proxy extends Thread {

    public static final int DEFAULT_LOCAL_PORT = 9000;
    public static final String DEFAULT_LOCAL_INTERFACE = "127.0.0.1";
    
    public static final String LOCAL_PORT_PROPERTY_NAME = "local-port";
    public static final String LOCAL_INTERFACE_PROPERTY_NAME = "local-interface";
    
    public static final int LOCAL_PORT;
    public static final String LOCAL_INTERFACE;
    
    static {
        String value = System.getProperty(LOCAL_PORT_PROPERTY_NAME);
        LOCAL_PORT = value == null ? DEFAULT_LOCAL_PORT : Integer.parseInt(value);
        
        value = System.getProperty(LOCAL_INTERFACE_PROPERTY_NAME);
        LOCAL_INTERFACE = value == null ? DEFAULT_LOCAL_INTERFACE : value;
    }
    
	List<RequestResponse> requestResponseList = new ArrayList<RequestResponse>();
	
	ServerSocket proxyServerSocket;
	
	public void run() {
		try {
            ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
			proxyServerSocket = serverSocketFactory.createServerSocket(LOCAL_PORT, 10000, InetAddress.getByName(LOCAL_INTERFACE));
			ServerSocketHandler handler = new ServerSocketHandler(proxyServerSocket, false);
			handler.start();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class ServerSocketHandler extends Thread {
		ServerSocket serverSocket;
		boolean secure;
		
		ServerSocketHandler(ServerSocket serverSocket, boolean secure) {
			this.serverSocket = serverSocket;
			this.secure = secure;
		}
		
		public void run() {
			try {
				while (true) {
					Socket socket = serverSocket.accept();
					RequestHandler handler = new RequestHandler(socket);
					handler.start();						
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	class RequestHandler extends Thread {
		Socket socket;
		
		RequestHandler(Socket clientSocket) {
			this.socket = clientSocket;
		}
		
		public void run() {
			try {
				InputStream clientIs = socket.getInputStream();
				OutputStream clientOs = socket.getOutputStream();
				
				
				InputStreamReader isr = new InputStreamReader(clientIs);
				BufferedReader br = new BufferedReader(isr);
				
				String line = null;
				StringBuilder buffer = new StringBuilder();
				String method = null;
				String urlText = null;
				boolean first = true;
				while ( (line = br.readLine()) != null) {
					
					if (first) {
						first = false;
						String chunks[] = line.split(" ");
						method = chunks[0];
						urlText = chunks[1];
					}
					
					buffer.append(line + "\r\n");
					if (line.equals(""))
					    break;
				}
				
				if ("connect".equalsIgnoreCase(method)) {
					String connectResponse = "HTTP/1.0 200 Connection established\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";

					clientOs.write(connectResponse.getBytes());
					clientOs.flush();
					
					boolean isHttps = false;
					if (!urlText.startsWith("http")) {
						isHttps = urlText.endsWith("443");
						urlText = (isHttps ?  "https://" : "http://") + urlText;
					}
					URL url = new URL(urlText);
					
					SSLSocketFactory secureSocketFactory = CertUtil.getTunnelSSLSocketFactory(url.getHost());
					SSLSocket sslSocket = (SSLSocket) secureSocketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
					sslSocket.setUseClientMode(false);
					try {
						sslSocket.startHandshake();
					} catch (Exception e) {
					    System.err.println("Error on URL: " + urlText);
						throw new RuntimeException(e);
					}
					
					InputStream is = sslSocket.getInputStream();
					Request request = new Request(Message.parseMessage(is, true));
					
					SSLContext ctx = SSLContext.getDefault();
					SSLSocketFactory factory = ctx.getSocketFactory();
					Socket targetSocket = factory.createSocket(request.host, 443);
					OutputStream targetOs = targetSocket.getOutputStream();
					targetOs.write(request.message.httpHeader.bytes);
					targetOs.write(request.message.body.rawBytes);
					targetOs.flush();
					
					InputStream targetIs = targetSocket.getInputStream();
	                Response response = new Response(Message.parseMessage(targetIs, false));
					final RequestResponse rr = new RequestResponse(request, response);
					Thread thread = new Thread() {
						public void run() {
							try { DataManager.INSTANCE.add(rr); } 
							catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					thread.start();

					
	                sslSocket.getOutputStream().write(response.message.httpHeader.bytes);
	                sslSocket.getOutputStream().write(response.message.body.rawBytes);
					sslSocket.getOutputStream().flush();
					sslSocket.close();
				}
				else if (urlText != null) {
					System.out.println("ATTEMPTING URL: " + urlText);
					URL url = new URL(urlText);
					
                    Request request = new Request(Message.parseMessage(new ByteArrayInputStream(buffer.toString().getBytes()), true));

                    SocketFactory socketFactory = SocketFactory.getDefault();
                    int port = url.getPort();
                    Socket targetSocket = socketFactory.createSocket(url.getHost(), port < 0 ? 80 : port);
                    OutputStream targetOs = targetSocket.getOutputStream();
                    targetOs.write(request.message.httpHeader.bytes);
                    targetOs.write(request.message.body.rawBytes);
                    targetOs.flush();
                    
                    InputStream targetIs = targetSocket.getInputStream();
                    Response response = new Response(Message.parseMessage(targetIs, false));
                    final RequestResponse rr = new RequestResponse(request, response);
                    Thread thread = new Thread() {
                        public void run() {
                            try { DataManager.INSTANCE.add(rr); } 
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    thread.start();

                    socket.getOutputStream().write(response.message.httpHeader.bytes);
                    socket.getOutputStream().write(response.message.body.rawBytes);
                    socket.getOutputStream().flush();
                    socket.close();
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	class StringIoHandler {
		public void handle(InputStream clientIs, OutputStream clientOs, InputStream targetIs, OutputStream targetOs) throws IOException {

			String line;
			StringBuilder buffer = new StringBuilder();

			InputStreamReader isr = new InputStreamReader(targetIs);
			BufferedReader br = new BufferedReader(isr);
			while ( (line = br.readLine()) != null && !line.equals("")) {
				buffer.append(line + "\n");
			}
			
//			System.out.println("->clientOs: " + buffer);
			
			clientOs.write(buffer.toString().getBytes());
			clientOs.flush();

			buffer.setLength(0);
			
			isr = new InputStreamReader(clientIs);
			br = new BufferedReader(isr);
			while ( (line = br.readLine()) != null && !line.equals("")) {
				buffer.append(line + "\n");
			}

			targetOs.write(buffer.toString().getBytes());
			targetOs.flush();
			
		}
	}
	
	public static void main(String[] args) throws Exception {
		Proxy proxy = new Proxy();
		proxy.start();
	}
}

