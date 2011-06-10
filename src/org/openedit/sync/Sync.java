package org.openedit.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.openedit.uploader.HttpUploader;
import org.openedit.uploader.Upload;

public class Sync implements Runnable
{
	private static final long serialVersionUID = 1L;
	protected String fieldCookies;
	protected String fieldUploadUrl;
	protected String fieldListUrl;
	protected String fieldAppletName;
	
	public String getAppletName()
	{
		return fieldAppletName;
	}

	public void setAppletName(String inAppletName)
	{
		fieldAppletName = inAppletName;
	}

	protected HttpUploader fieldHttpUploader;

	public String getUploadUrl()
	{
		return fieldUploadUrl;
	}

	public void setUploadUrl(String inUploadUrl)
	{
		fieldUploadUrl = inUploadUrl;
	}
	public void run()
	{
		try
		{
			uploadFiles();
		}
		catch( Exception ex)
		{
			if( ex instanceof RuntimeException)
			{
				throw (RuntimeException)ex;
			}
			throw new RuntimeException(ex);
		}
	}
	public void uploadFiles() throws Exception
	{
		String resp = null; 
		Set done = new HashSet();
			while ( (resp = getPending() ) != null)
			{
				//grab the top one
				BufferedReader reader = new BufferedReader(new StringReader(resp));
				String str = null;
				//should be 
				while ((str = reader.readLine()) != null)
				{
					str = str.trim();
					if (str.startsWith("<asset ")) //there is only one at a time
					{
						//JOptionPane.showMessageDialog(this, "dowSnload " + str);
						Upload upload = createUpload(str);
						log("Sending " + upload.getLocalPath());
						if( upload.getSentSoFar() > 0)
						{
							//If we are picking up we want to be sure another applet is not in the middle of shutting down
//							done.add(upload.getLocalPath());
							Thread.currentThread().sleep(1000 * 60 * 1); //1 min sleep
						}
						
						if(done.contains(upload.getLocalPath()))
						{
							//file must be locked or there are errors
							Thread.currentThread().sleep(1000 * 60 * 2); //2 min sleep
						}
						done.add(upload.getLocalPath());
						//Dont try to send unless the file size matches
						if(upload.getExpectedSize() == upload.getTotalSize())
						{
							getHttpUploader().send(upload);
						}
						else
						{
							log("File size was not the same. Do not try to send:" + upload.getLocalPath());
							done.add(upload.getLocalPath());
						}
						//continue; //We will let the finish event try to upload more files
					}
				}
			}
	}
	protected void log( String inLog)
	{
		System.out.println(inLog);
	}
	protected Upload createUpload(String inStr)
	{
		///<asset id="$hit.id" catalogid="$!hit.catalogid" localpath="$!hit.localpath" />
		int aidstart = inStr.indexOf("\"") + 1;
		int aidend = inStr.indexOf("\"", aidstart + 1);
		String sourcepath = inStr.substring(aidstart, aidend);

		int catstart = inStr.indexOf("\"", aidend + 1) + 1;
		int catend = inStr.indexOf("\"", catstart + 1);
		String catalogid = inStr.substring(catstart, catend);

		int locstart = inStr.indexOf("\"", catend + 1) + 1;
		int locaend = inStr.indexOf("\"", locstart + 1);
		String localpath = inStr.substring(locstart, locaend);

		int offsets = inStr.indexOf("\"", locaend + 1) + 1;
		int offsete = inStr.indexOf("\"", offsets + 1);
		long offset = 0;
		if( offsete > offsets)
		{
			String soffset = inStr.substring(offsets, offsete);
			offset = Long.valueOf(soffset);
		}
		
		int sizes = inStr.indexOf("\"", offsete + 1) + 1;
		int sizee = inStr.indexOf("\"", sizes + 1);
		long size = 0;
		if( sizee > sizes)
		{
			String ssize = inStr.substring(sizes, sizee);
			size = Long.valueOf(ssize);
		}
		try
		{
			return createUpload(sourcepath, catalogid, localpath, offset, size);
		}
		catch( MalformedURLException ex)
		{
			throw new RuntimeException(ex);
		}

	}

	protected Upload createUpload(String inSourcePath, String inCatalogId, String inLocalPath, long inOffset, long inSize) throws MalformedURLException
	{
		//TODO: Add assetid and catalogid params
		final Upload upload = new Upload();
		upload.setStartSendingTime(System.currentTimeMillis());
		upload.setLocalPath(inLocalPath);
		upload.setCookies(getCookies());
		upload.setOffset(inOffset);
		File toupload = new File(inLocalPath);
		upload.setTotalSize(toupload.length());
		upload.setServerPostURL(getUploadUrl());
		upload.setSaveToFileName(toupload.getName());
		upload.setExpectedSize(inSize);
		upload.addParameter("catalogid", inCatalogId);
		upload.addParameter("sourcepath", inSourcePath);
		upload.addParameter("totalsize", String.valueOf( toupload.length() ) );
		upload.addHeader("x-appletname", getAppletName());
		upload.addHeader("x-destinationpath", "/WEB-INF/data/" + inCatalogId + "/temp/" + inSourcePath);
		upload.addHeader("x-seekrange", String.valueOf(inOffset));
		
		return upload;
	}

	public String getPending()
	{
		HttpHelper http = new HttpHelper();
		// Get the pending assets list
		try
		{
			String listPath = getListUrl();
			URL url = new URL(listPath);
			String returned = http.getContent(url,getCookies());
			System.out.println("Connecting to " + url.toString());
			
			if(returned == null || returned.length() < 50)
			{
				return null;
			}
			return returned.toString();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error reading from site: " + e.getLocalizedMessage());
		}
	}

	public String getCookies()
	{
		return fieldCookies;
	}

	public void setCookies(String cookies)
	{
		fieldCookies = cookies;
	}

	public String getListUrl()
	{
		return fieldListUrl;
	}

	public void setListUrl(String listPath)
	{
		fieldListUrl = listPath;
	}

	public HttpUploader getHttpUploader()
	{
		if (fieldHttpUploader == null)
		{
			fieldHttpUploader = new HttpUploader();
			//fieldHttpUploader.addProgressListener(this);
		}

		return fieldHttpUploader;
	}

	public void setHttpUploader(HttpUploader inHttpUploader)
	{
		fieldHttpUploader = inHttpUploader;
	}

	public void fileFinished(Upload inUpload)
	{
		//Update the status of how many uploads are left to do
	}

	public void sentBytes(Upload inUpload)
	{
		/* Non of these are used
		inUpload.getMacPath()
		inUpload.getCount()
		inUpload.getSaveToFileName()
		inUpload.getSaveToPath()
		inUpload.getSaveToSubFolders()
		*/
	}

}
