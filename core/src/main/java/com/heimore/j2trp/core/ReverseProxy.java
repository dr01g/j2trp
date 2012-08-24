package com.heimore.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReverseProxy extends HttpServlet {
	
	
	private String targetHost;
	private int targetPort;
	private static final long serialVersionUID = 1L;
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final byte[] CR_LF = new byte[] { (byte) 0x0d, (byte) 0x0a };
	private static final int[] WELL_KNOWN_PORT = new int[] { 80, 443 }; // Array must be sorted.

    public ReverseProxy() {
    }

    @SuppressWarnings("unchecked")
    protected static void copyHeaders (OutputStream ps, HttpServletRequest request) throws IOException {
    	for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
    		String headerName = (String) headers.nextElement();
    		if (!headerName.equalsIgnoreCase("Host")) {
        		for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
        			print(ps, headerName);
        			print(ps, ": ");
        			print(ps, headerValues.nextElement());
        			crlf(ps);
        		}
    		}
    		
    	}
    }
    
    static boolean isWellKnownPort (int port) {
    	return (Arrays.binarySearch(WELL_KNOWN_PORT, port) >= 0);
    }
    
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp);
	}

	static void print (OutputStream os, String data) throws IOException {
		os.write(data.getBytes(ASCII));
	}
	
	static void crlf (OutputStream os) throws IOException {
		os.write(CR_LF);
	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] bodyBuffer = new byte[1024];
		try {
			socket = new Socket(targetHost, targetPort);
			ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
			// PrintStream ps = new PrintStream(socket.getOutputStream());
			print(headerBuffer, request.getMethod());
			print(headerBuffer, " ");
			print(headerBuffer, request.getRequestURI());
			if (request.getQueryString() != null) {
				print(headerBuffer, "?");
				print(headerBuffer, request.getQueryString());
			}
			print(headerBuffer, " HTTP/1.0");
			crlf(headerBuffer);
			copyHeaders(headerBuffer, request);
			print(headerBuffer, "X-Forwarded-For: ");
			print(headerBuffer, request.getRemoteAddr());
			crlf(headerBuffer);
			print(headerBuffer, "Host: "); // Add port?
			print(headerBuffer, targetHost);
			if (!isWellKnownPort(targetPort)) {
				print(headerBuffer, ":");
				print(headerBuffer, String.valueOf(targetPort));
			}
			crlf(headerBuffer);
			crlf(headerBuffer);
			
			// If request is a POST, relay the body of the request.
			if (request.getMethod().equals("POST")) {
				InputStream is = request.getInputStream();
				int bytesRead = is.read(bodyBuffer);
				while (bytesRead != -1) {
					headerBuffer.write(bodyBuffer, 0, bytesRead);
					bytesRead = is.read(bodyBuffer);
				}
			}
			
			// Write output to target server.
			OutputStream targetOutputStream = socket.getOutputStream();
			byte[] output = headerBuffer.toByteArray();
			System.out.println(new String(output, ASCII));
			targetOutputStream.write(output);
			targetOutputStream.flush();

			// Read response from target server.
			InputStream proxiedInputSteam = socket.getInputStream();
			OutputStream clientsRespOs = response.getOutputStream();
			int bytesRead = proxiedInputSteam.read(bodyBuffer);
			while (bytesRead != -1) {
				clientsRespOs.write(bodyBuffer, 0, bytesRead);
				bytesRead = proxiedInputSteam.read(bodyBuffer);
			}
			targetOutputStream.close();
			clientsRespOs.close();
		
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (socket != null) {
				try {
					socket.close();
				}
				catch (IOException e) {
					// Don't care.
				}
			}
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		targetHost = config.getInitParameter("TARGET_HOST");
		targetPort = Integer.parseInt(config.getInitParameter("TARGET_PORT"));
	}

	
}