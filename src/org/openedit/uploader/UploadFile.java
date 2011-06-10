package org.openedit.uploader;

import java.util.HashMap;
import java.util.Map;

public class UploadFile {

	protected String fieldSaveToPath;
	protected String fieldSaveToSubFolders;
	protected String fieldSaveToFileName;
	protected String fieldLocalPath;
	protected boolean fieldRan = false;
	protected int fieldCount;
	protected long fieldStartSendingTime;
	protected long fieldSentSoFar;
	protected long fieldTotalSize;
	protected long fieldOffset;
	protected String fieldStatus;
	protected Map fieldRequestParameters;
	protected Map fieldHeaders;

	public long getOffset()
	{
		return fieldOffset;
	}

	public void setOffset(long inOffset)
	{
		fieldOffset = inOffset;
	}

	

	public UploadFile() {
		super();
	}

	public boolean isRan() {
		return fieldRan;
	}

	public void setRan(boolean inRan) {
		fieldRan = inRan;
	}

	public int getCount() {
		return fieldCount;
	}

	public void setCount(int inCount) {
		fieldCount = inCount;
	}

	public long getStartSendingTime() {
		return fieldStartSendingTime;
	}

	public void setStartSendingTime(long inStartSendingTime) {
		fieldStartSendingTime = inStartSendingTime;
	}

	public long getSentSoFar() {
		return fieldSentSoFar;
	}

	public void setSentSoFar(long inSentSoFar) {
		fieldSentSoFar = inSentSoFar;
	}

	public long getTotalSize() {
		return fieldTotalSize;
	}

	public void setTotalSize(long inTotalSize) {
		fieldTotalSize = inTotalSize;
	}

	public String getSaveToSubFolders() {
		return fieldSaveToSubFolders;
	}

	public void setSaveToSubFolders(String inSaveToSubFolders) {
		fieldSaveToSubFolders = inSaveToSubFolders;
	}

	public String getSaveToFileName() {
		return fieldSaveToFileName;
	}

	public void setSaveToFileName(String inSaveToFileName) {
		fieldSaveToFileName = inSaveToFileName;
	}

	public String getStatus() {
		return fieldStatus;
	}

	public void setStatus(String inStatus) {
		fieldStatus = inStatus;
	}

	public boolean isFinished() {
		return (getTotalSize() == getSentSoFar());
	}
	public void addParameter(String inKey, String inValue)
	{
		getRequestParameters().put( inKey, inValue);
	}
	
	public Map getRequestParameters()
	{
		if (fieldRequestParameters == null)
		{
			fieldRequestParameters = new HashMap();
		}

		return fieldRequestParameters;
	}

	public void setRequestParameters(Map inRequestParameters)
	{
		fieldRequestParameters = inRequestParameters;
	}
	
	public void addHeader(String inKey, String inValue)
	{
		getHeaders().put( inKey, inValue);
	}
	
	public Map getHeaders()
	{
		if (fieldHeaders == null)
		{
			fieldHeaders = new HashMap();
		}

		return fieldHeaders;
	}
}