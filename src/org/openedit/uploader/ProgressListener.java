package org.openedit.uploader;

public interface ProgressListener
{
	void sentBytes(Upload upload);
	void fileFinished(Upload inUpload);
}
