package org.openedit.uploader;



public class Upload extends UploadFile
{
	//protected JProgressBar fieldProgressBar;
	//Maybe we will ask the server on a case by case basis. 
	//Get the size of a URL to compare
	protected String fieldServerPostURL;
	protected String fieldCookies;
	protected String fieldRemotePath;
	protected String fieldMacPath;
	protected long fieldExpectedSize;
	public String getMacPath()
	{
		return fieldMacPath;
	}
	public void setMacPath(String inMacPath)
	{
		fieldMacPath = inMacPath;
	}
	public String getServerPostURL()
	{
		return fieldServerPostURL;
	}
	public void setServerPostURL(String inServerPostURL)
	{
		fieldServerPostURL = inServerPostURL;
	}
	public String getSaveToPath()
	{
		return fieldSaveToPath;
	}
	public void setSaveToPath(String inSaveToPath)
	{
		fieldSaveToPath = inSaveToPath;
	}
	public String toFullSavePath()
	{
		return getSaveToPath() + getSaveToSubFolders() + getSaveToFileName();
	}
	public String getCookies()
	{
		return fieldCookies;
	}
	public void setCookies(String inCookies)
	{
		fieldCookies = inCookies;
	}

	public String getLocalPath()
	{
		return fieldLocalPath;
	}
	public void setLocalPath(String inLocalPath)
	{
		fieldLocalPath = inLocalPath;
	}
	public String getRemotePath()
	{
		return fieldRemotePath;
	}
	public void setRemotePath(String inRemotePath)
	{
		fieldRemotePath = inRemotePath;
	}
	public long getExpectedSize()
	{
		return fieldExpectedSize;
	}
	public void setExpectedSize(long inExpectedSize)
	{
		fieldExpectedSize = inExpectedSize;
	}
	
	
}
