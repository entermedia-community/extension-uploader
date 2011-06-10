package org.openedit.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import org.openedit.uploader.Upload;
import org.openedit.uploader.ssl.NaiveTrustManager;

public class HttpHelper
{

	public String getContent(URL inUrl, String inCookies) throws Exception
	{
	/*	
		URLConnection con = inUrl.openConnection();
		con.setRequestProperty("Cookie", inCookies);
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                con.getInputStream()));
	*/	
		Socket socket = connect(inUrl);
		Writer out = new PrintWriter(socket.getOutputStream() );
		writeStandardHeaders(inUrl, inCookies, out);
		BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		String status = in.readLine();
		if( !status.startsWith("HTTP/1.1 200"))
		{
			throw new RuntimeException("Could not GET: " + status + " " + inUrl);
		}

        String content =  readBody(in);

		return content;
	}
	
	public Socket connect(URL inUrl) throws Exception
	{
		Socket socket = null;
		//Not using connections so that I can set the Cookies and not follow redirects
		if( "https".equals( inUrl.getProtocol() ) )
		{
			int port = inUrl.getPort();
			if( port < 0)
			{
				port = 443;
			}
			socket = NaiveTrustManager.getSocketFactory().createSocket(inUrl.getHost(),port );
		}
		else
		{
			int port = inUrl.getPort();
			if( port < 0)
			{
				port = 80;
			}
			socket = new Socket(inUrl.getHost(),port);
		}
		return socket;
	}
	
//	public String readContent(Socket inSocket) throws Exception
//	{
//		BufferedReader in = new BufferedReader( new InputStreamReader( inSocket.getInputStream() ) );
//
//		return readStream(in);
//	}

	public String readStatus(BufferedReader in) throws IOException
	{
		return in.readLine();
	}

	
	public String readBody(BufferedReader in) throws IOException
	{
		StringBuffer returned = new StringBuffer();
		char c = 0;
		char c1 = 0;
		char c2 = 0;
		char c3 = 0;
		char c4 = 0;
		//need to remove double new lines
		boolean startbody = false;
		while ( ( c = (char)in.read() ) != -1)
		{
			//stop when we get to r n r n
			c1 = c2;
			c2 = c3;
			c3 = c4;
			c4 = c;
			if( c1 == '\r' && c2 == '\n' && c3 == '\r' && c4 == '\n')
			{
				if( startbody == true)
				{
					returned.delete(returned.length() - 3, returned.length());
					break;					
				}
				else
				{
					startbody = true;
				}
			}
			if( startbody)
			{
				returned.append(c);
			}
		}
		return returned.toString();
	}
	
	public void writeStandardHeaders(URL inURL, String cookies, Writer out) throws Exception
	{
		out.write("GET " + inURL.getPath() + " HTTP/1.1\r\n");
		out.write("User-Agent: OpenEditUpload/1.0\r\n");
		out.write("Host: " + inURL.getHost() + "\r\n" );
		out.write("Cookie: " + cookies + "\r\n");
		out.write("\r\n");
		out.flush();
	}
	
	public void addHeader(URL inURL, String cookies, Writer out, Map inCustomHeaders) throws UnsupportedEncodingException, IOException
	{
		out.write("User-Agent: OpenEditUpload/1.0\r\n");
		out.write("Host: " + inURL.getHost() + "\r\n" );
		out.write("Cookie: " + cookies + "\r\n");
		if(inCustomHeaders != null)
		{
			for (Iterator iterator = inCustomHeaders.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String)iterator.next();
				out.write(key + ": " + inCustomHeaders.get(key) + "\r\n");
			}
		}
	}
	public Socket createSocket(URL inURL) throws IOException, UnknownHostException
	{
		Socket socket = null;
		
		//Not using connections so that I can set the Cookies and not follow redirects
		if( "https".equals( inURL.getProtocol() ) )
		{
			int port = inURL.getPort();
			if( port < 0)
			{
				port = 443;
			}
			//Security.get
//			 Security.addProvider(
//				      new com.sun.net.ssl.internal.ssl.Provider());

			socket = NaiveTrustManager.getSocketFactory().createSocket(inURL.getHost(),port );
		}
		else
		{
			int port = inURL.getPort();
			if( port < 0)
			{
				port = 80;
			}
			socket = new Socket(inURL.getHost(),port);
		}
		return socket;
	}
	
	public String getFileHeader(String BOUNDARY, Upload inUpload)
	{
		StringBuffer done = new StringBuffer();
		done.append("\r\n--" + BOUNDARY + "\r\n");
		done.append("Content-Disposition: form-data; name=\"file.0\"; filename=\""+ inUpload.getSaveToFileName() +"\"\r\n");
		done.append("Content-Type: application/octet-stream\r\n");
		
		//Comment in for GZIP
		//done.append("Content-Encoding: gzip\r\n");
		done.append("\r\n");
		return done.toString();
	}
	public String getVariable(String BOUNDARY,String inKey,String inValue)
	{
		StringBuffer done = new StringBuffer();
		done.append("\r\n--" + BOUNDARY + "\r\n");
		done.append("Content-Disposition: form-data; name=\"" + inKey + "\"\r\n");
		done.append("\r\n");
		done.append(inValue);
		//done.append("\n");
		//done.append("\n--" + BOUNDARY + "\n");
		return done.toString();
	}
	public String getFooter(String BOUNDARY, File inFile)
	{
		StringBuffer done = new StringBuffer();
		done.append("\r\n--" + BOUNDARY + "--\r\n");
		//done.append("\r\n");

		return done.toString();
	}
	public String getParams(String BOUNDARY, Map inUpload)
	{
		StringBuffer out = new StringBuffer();
		for (Iterator iterator = inUpload.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			String value = (String)inUpload.get(key);
			String addvariable = getVariable(BOUNDARY, key, value);
			out.append(addvariable);
		}
		return out.toString();
	}

	
	
}
