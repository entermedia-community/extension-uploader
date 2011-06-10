package org.openedit.uploader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class CopyFileUploader extends HttpUploader
{

	public void send(Upload inUpload) throws Exception
	{
		File inFile = new File(inUpload.getLocalPath());
		String os = System.getProperty("os.name").toLowerCase();
		String inOutputfile = null; 
		if(os.contains("mac"))
		{
			inOutputfile = inUpload.getMacPath();
		}
		else
		{
		 inOutputfile = inUpload.getRemotePath();
		}
		if(inOutputfile == null){
			inOutputfile = inUpload.getRemotePath();
		}

		if(!inOutputfile.endsWith("/"))
		{
			inOutputfile = inOutputfile + "/";
		}
		inOutputfile = inOutputfile + inFile.getName();
		File outf = new File(inOutputfile );
		System.out.println("Copy file " + inFile  + " to " + outf);
		
		OutputStream output = new FileOutputStream(outf);
		//Writer out = new OutputStreamWriter(output, "UTF-8");
		inUpload.setTotalSize(inFile.length());
		InputStream input = new FileInputStream(inFile);
		try
		{
			byte[] bytes = new byte[60000];
			int iRead = -1;
			inUpload.setStartSendingTime(System.currentTimeMillis());
			while (isRun())
			{
				iRead = input.read(bytes);
				if (iRead == -1)
				{
					break;
				}
				else
				{
					output.write(bytes, 0, iRead);
					long sentsofar = inUpload.getSentSoFar() + iRead;

					inUpload.setSentSoFar(sentsofar);
					for (int i = 0; i < getProgressListeners().size(); i++)
					{
						ProgressListener listener = (ProgressListener) getProgressListeners().get(i);
						listener.sentBytes(inUpload);
					}
				}
			}
		}
		finally
		{
			//to use gzip, change these this line 
			//	gzip.finish();
			if (input != null)
			{
				input.close();
			}
			output.flush();
			output.close();

		}
		//out.flush();
		double time = (System.currentTimeMillis() - inUpload.getStartSendingTime()) / 1000D;
		//args[2] = String.valueOf(time);
		System.out.println("Finished " + inUpload.getTotalSize() + " in " + time  + " seconds");

		//out.close();
		for (int i = 0; i < getProgressListeners().size(); i++)
		{
			ProgressListener listener = (ProgressListener) getProgressListeners().get(i);
			listener.fileFinished(inUpload);
		}
	}

}
