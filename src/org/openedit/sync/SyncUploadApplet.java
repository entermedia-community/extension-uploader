package org.openedit.sync;

import javax.swing.JApplet;

import netscape.javascript.JSObject;

import org.openedit.uploader.ProgressListener;
import org.openedit.uploader.Upload;

public class SyncUploadApplet extends JApplet implements ProgressListener
{
	private static final long serialVersionUID = 1L;
	protected Sync fieldSync;
	protected JSObject fieldJSO;
	
	public Sync getSync()
	{
		if (fieldSync == null)
		{
			fieldSync = new Sync();
			fieldSync.getHttpUploader().addProgressListener(this);
		}
		return fieldSync;
	}

	public void setSync(Sync inSync)
	{
		fieldSync = inSync;
	}

	public void start()
	{
		super.start();
    	System.out.println("Sync loaded version " + getClass().getPackage().getImplementationVersion() );

		//pull next file from here
		getSync().setListUrl(getParameter("listurl"));

		//where we upload to 
		getSync().setUploadUrl(getParameter("uploadurl"));

		getSync().setCookies(getParameter("cookies"));

		getSync().setAppletName(getParameter("appletname"));

		Runnable run = new Runnable()
		{
			public void run()
			{
				try
				{
					getSync().uploadFiles();
					showComplete();
				}
				catch ( Exception ex)
				{
					handleError(ex);
				}
			}
		};
		new Thread(run).start();
	}
	private void handleError(Exception inE)
	{
		//JOptionPane.showMessageDialog(this,"Error " +  inE);
		//System.out.println("handleError: " + inE);
		inE.printStackTrace();
	}

	public void showComplete()
	{
		String[] args = new String[1];
		args[0] = "";
		getJSObject().call("uploadsComplete", args);
	}

	public JSObject getJSObject()
	{
		if (fieldJSO == null)
		{
			fieldJSO = JSObject.getWindow(this);
		}
		return fieldJSO;
	}

	
	public void fileFinished(Upload inUpload)
	{
		//Update the status of how many uploads are left to do
		String[] args = new String[1];
		args[0] = inUpload.getRemotePath();
		getJSObject().call("filefinished", args);
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

	public void stop()
	{
		super.stop();
		getSync().getHttpUploader().setRun(false);
	}
}
