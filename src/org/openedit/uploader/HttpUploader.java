package org.openedit.uploader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openedit.sync.HttpHelper;

public class HttpUploader
{
	protected List fieldProgressListeners;
	protected boolean fieldRun = true;
	protected HttpHelper fieldHttpClient;
	protected int fieldBufferSize = 40000;
	
	
	public HttpUploader()
	{
	}
	
	
	public int getBufferSize()
	{
		return fieldBufferSize;
	}

	public void setBufferSize(int inBufferSize)
	{
		fieldBufferSize = inBufferSize;
	}

	public HttpHelper getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			fieldHttpClient = new HttpHelper();
		}

		return fieldHttpClient;
	}

	public void setHttpClient(HttpHelper inHttpClient)
	{
		fieldHttpClient = inHttpClient;
	}

	public void send(Upload inUpload) throws Exception
	{
		File inFile  = new File( inUpload.getLocalPath() );
		String upath =inUpload.getServerPostURL();
		URL inURL = new URL( upath);
		String cookies = inUpload.getCookies();
		
		Socket socket = getHttpClient().createSocket(inURL);
		//System.out.println("SO Linger"  + socket.getSoLinger() );
		if (socket.isConnected())
		{
			System.out.println("Uploading to " + inURL);
			BufferedReader fromserver = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

			//Buffers in Windows tends to slow down things
			OutputStream toserver = socket.getOutputStream();

			Writer headers = new OutputStreamWriter( toserver,"UTF-8" );
			headers.write("POST " + inURL.getPath() + " HTTP/1.1\r\n");
			getHttpClient().addHeader(inURL, cookies, headers, inUpload.getHeaders());
			
			
			String BOUNDARY = "---------------------------186239751236782178940581817";
			headers.write("Content-Type: multipart/form-data; boundary=" + BOUNDARY + "\r\n");

			String sectionheader  = getHttpClient().getFileHeader( BOUNDARY, inUpload );
			String addvariable = getHttpClient().getVariable(BOUNDARY, "path", inUpload.getSaveToPath() + inUpload.getSaveToSubFolders());
			String footer = getHttpClient().getFooter( BOUNDARY, inFile );
			String params = getHttpClient().getParams(BOUNDARY, inUpload.getRequestParameters());
			inUpload.setTotalSize(inFile.length());
			
			long offset = inUpload.getOffset();
			
			long total = sectionheader.length() + inUpload.getTotalSize() - offset + addvariable.length() + params.length() + footer.length(); //--
			
			headers.write("Content-Length: " + total + "\r\n");
			headers.write(sectionheader);
			headers.flush();
			//Send Data
			BufferedInputStream input = new BufferedInputStream( new FileInputStream(inFile) );
			if( offset > 0)
			{
				input.skip(offset);
				inUpload.setSentSoFar(offset);
			}
		//to use gzip, change these this line 
			//GZIPOutputStream gzip = null;
			OutputStream gzip= null;
			try
			{
				byte[] bytes = new byte[getBufferSize()];
				int iRead = -1;
				//long sentsofar = 0;
				//to use gzip, change these this line 
				 //gzip = new GZIPOutputStream(output);
				 gzip = toserver;
				inUpload.setStartSendingTime(System.currentTimeMillis());
//				int subtotal = 5000;
				while (isRun())
				{
					iRead = input.read(bytes);
					if (iRead == -1)
					{
						break;
					}
					else
					{
						gzip.write(bytes, 0, iRead);
						if( total < 100000)
						{
							//flush as we go so we see realistic progress
							//for larger files we assume windows will slow down after its 60k buffer is full
							gzip.flush(); 
						}
						//On windows if we do not flush then it tells we are sending stuff when we are not really
						//sending anything
						long sentsofar = inUpload.getSentSoFar() + iRead;
	//					subtotal = subtotal +  iRead;
						inUpload.setSentSoFar(sentsofar);
						for (int i = 0; i < getProgressListeners().size(); i++)
						{
							ProgressListener listener = (ProgressListener)getProgressListeners().get(i);
							listener.sentBytes(inUpload);
						}
					}
				}
			}
			finally
			{
				//to use gzip, change these this line 
			//	gzip.finish();
				if( input != null )
				{
					input.close();
				}
			}
			toserver.flush();
			if( params.length() > 0)
			{
				headers.write(params);
			}
			headers.write(addvariable);
			headers.write(footer);
			headers.write("\r\n");
			headers.flush();

			
			String response = getHttpClient().readStatus(fromserver);
			inUpload.setStatus( response);
			if( response == null || !response.startsWith("HTTP/1.1 200"))
			{
				throw new RuntimeException("Could not upload: " + response + " " + inURL);
			}
			
			headers.close();
			for (int i = 0; i < getProgressListeners().size(); i++)
			{
				ProgressListener listener = (ProgressListener)getProgressListeners().get(i);
				listener.fileFinished(inUpload);
			}
		}
		else
		{
			throw new Exception("Could not connect to server: " + inURL.toString());
		}
		
	}

	
	public void addProgressListener(ProgressListener inListener)
	{
		getProgressListeners().add(inListener);
	}
	public List getProgressListeners()
	{
		if (fieldProgressListeners == null)
		{
			fieldProgressListeners = new ArrayList();
		}
		return fieldProgressListeners;
	}
	/*
	//Pass in a URL with the filename on the end. It checks that this is not already uploaded.
	public boolean alreadyExists(String cookies, URL inURL, String inFilename) throws Exception
	{
		//Do a post and check the return text
		Socket socket = createSocket(inURL);		
		OutputStream output = socket.getOutputStream();
		Writer out = new OutputStreamWriter( output,"UTF-8" );
		String path = inURL.getPath() + inFilename;
		out.write("GET " + path + " HTTP/1.1\r\n");
		addHeader(inURL, cookies, out);
		out.write("\r\n");
		String response = readResponse(socket);
		if( response.contains("404"))
		{
			return false;
		}
		if( response.contains("already exists")) //Maybe we should return the size?
		{
			return true;
		}
		return false;
	}
	*/
	public boolean isRun()
	{
		return fieldRun;
	}
	public void setRun(boolean inRun)
	{
		fieldRun = inRun;
	}

	
}