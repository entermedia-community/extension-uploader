package org.openedit.uploader;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 * 1. Show progress bar as the data is sent over the wire 2. Add a count 1 of
 * 23images 3. Might want to add a paste button if applicable. Also might want
 * to show a browse button with multi-select enabled 4. Oh and another nice
 * thing... Check that the images are not already uploaded before being sent 5.
 * Add JPG Compression
 * 
 * @author cburkey
 */

public class UploadApplet extends JApplet implements DropTargetListener, ProgressListener
{
	private static final long serialVersionUID = 1L;
	protected DropTarget dropTarget;
	protected JLabel dropHereLabel;
	protected JButton fieldSelectButton;
	protected JSObject fieldJSO;
	protected HttpUploader fieldMultiPartUpload;
	protected CopyFileUploader fieldCopyFileUploader;
	protected List fieldUploads;
	protected English fieldEnglish;
	protected Timer fieldTimer;
	protected Timer fieldUiTimer;
	protected Boolean fieldInBrowser;
	protected static DataFlavor urlFlavor, uriListFlavor, macPictStreamFlavor;
	protected File fieldLastSelectedDirectory;
	protected long fieldLastSent;
	
	public void init()
	{
		super.init();
		getContentPane().setBackground(Color.WHITE);
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			System.out.println("look and feel :" + e);
		}
		try
		{
			urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
			uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
		}
		catch ( Exception ex)
		{
			System.out.println("Drag and drop:" + ex);
		}
	}
	
	public void start()
	{
		super.start();
    	System.out.println("Upload Applet loaded version " + getClass().getPackage().getImplementationVersion() );

		String localPath = getParameter("localpath");
		if (localPath != null)
		{
			addUpload(0, localPath);
		}
		else
		{
			setLayout(new GridBagLayout());
			if (getParameter("uploadurl") == null)
			{
				setContentPane(new JLabel("uploadurl and savepath are \nrequired applet parameters"));
				return;
			}
			// set up drop target stuff
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.BOTH;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(5, 5, 5, 5);
			c.weightx = 0;
			c.weighty = 0;

			GridBagConstraints b = (GridBagConstraints) c.clone();
			b.weightx = 1;
			add(getLabel(), b);

			b = (GridBagConstraints) c.clone();
			b.gridx = 1;
			b.fill = GridBagConstraints.NONE;
			b.anchor = GridBagConstraints.NORTH;

			add(getSelectButton(), b);
		}

	}

	protected JButton getSelectButton()
	{
		if (fieldSelectButton == null)
		{
			fieldSelectButton = new JButton("Select files...");
			fieldSelectButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JFileChooser chooser = new JFileChooser();
					chooser.setFileHidingEnabled(false);
					chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					chooser.setMultiSelectionEnabled(true);
					chooser.setDialogType(JFileChooser.OPEN_DIALOG);
					chooser.setDialogTitle("Choose files or folders...");
					chooser.setFileFilter(new FileFilter()
					{
						public boolean accept(File inF)
						{
							return !inF.getName().startsWith(".");
						}

						public String getDescription()
						{
							return "Files or Folders";
						}
					});
					//chooser.setSelectedFile(file);
					if (getLastSelectedDirectory() != null)
					{
						chooser.setSelectedFile(getLastSelectedDirectory());
					}

					if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
					{
						File[] targetFiles = chooser.getSelectedFiles();
						for (int i = 0; i < targetFiles.length; i++)
						{
							addUpload(0, targetFiles[i].getAbsolutePath());
						}
					}
				}
			});
		}
		return fieldSelectButton;
	}

	protected void addUpload(int inFolderLevel, String inAbsolutePath)
	{
		File found = new File(inAbsolutePath);
		if (found.isDirectory())
		{
			File[] children = found.listFiles();
			inFolderLevel++;
			for (int i = 0; i < children.length; i++)
			{
				addUpload(inFolderLevel, children[i].getAbsolutePath());
			}
			return;
		}
		final Upload upload = new Upload();
		upload.setStartSendingTime(System.currentTimeMillis());
		getUploads().add(upload);
		upload.setCount(getUploads().size());

		upload.setLocalPath(inAbsolutePath);
		String cookies = getParameter("cookies");
		upload.setCookies(cookies);
		File toupload = new File(inAbsolutePath);
		setLastSelectedDirectory(toupload);
		upload.setTotalSize(toupload.length());

		String url = getParameter("uploadurl");
		if (url != null)
		{
			upload.setServerPostURL(url);
		}
		else
		{
			String path = getParameter("remotepath");
			String mac = getParameter("macpath");
			// check link
			upload.setRemotePath(path);
			upload.setMacPath(mac);
			
			path = checkRemotePath(upload);
			if(path == null)
			{
				return;
			}
		}

		String basesavepath = getParameter("savepath");
		if (!basesavepath.endsWith("/"))
		{
			basesavepath = basesavepath + "/";
		}
		upload.setSaveToPath(basesavepath);

		//Add in X levels of folders
		File parentfolder = toupload.getParentFile();
		// /upload/Small/Sub1/
		String folders = "";
		for (int i = 0; i < inFolderLevel; i++)
		{
			folders = parentfolder.getName() + "/" + folders;
			parentfolder = parentfolder.getParentFile();
		}
		if (folders.length() > 0 && !folders.endsWith("/"))
		{
			folders = folders + "/";
		}
		upload.setSaveToSubFolders(folders);
		
		String savetofilename = getParameter("savetofilename");
		if(savetofilename != null && savetofilename.length() > 0)
		{
			int extensionindex = toupload.getName().lastIndexOf(".");
			if(extensionindex != -1)
			{
				String extension = toupload.getName().substring(extensionindex);
				upload.setSaveToFileName(savetofilename + extension);
			}
			else
			{
				upload.setSaveToFileName(savetofilename + ".jpg");
			}
		}
		else
		{
			upload.setSaveToFileName(toupload.getName());
		}
		
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				try
				{
					if (upload.getServerPostURL() != null)
					{
						getMultiPartUpload().send(upload);
					}
					else
					{
						getCopyFileUploader().send(upload);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(UploadApplet.this, "error uploading " + ex);
				}
			}

		};
		getTimer().schedule(task, 0); // Puts them in order
	}

	
	private String checkRemotePath(Upload inUpload)
	{
		String os = System.getProperty("os.name").toLowerCase();
		String path = null;
		if (os.contains("mac"))
		{		
			path = inUpload.getMacPath();		
		}
		else
		{
			path = inUpload.getRemotePath();
		}
		File file = new File(path);
		if(file.exists())
		{
			return path;
		}
		int ret = JOptionPane.showConfirmDialog(UploadApplet.this, 
				"Path Not Available. Please check path availability and press OK:" + path 
				);
		if( ret == JOptionPane.OK_OPTION)
		{
			checkRemotePath(inUpload);			
		}
		return path;
	}

	protected void relabel()
	{
		int count = 0;
		for (Iterator iterator = getUploads().iterator(); iterator.hasNext();)
		{
			Upload upload = (Upload) iterator.next();
			if (upload.isFinished())
			{
				count++;
			}

		}

		getLabel().setText("uploaded " + count + " of " + getUploads().size() + " items");
		validate();
		repaint();
	}

	private JLabel getLabel()
	{
		if (dropHereLabel == null)
		{
			//Todo add a paste button
			dropHereLabel = new JLabel("", SwingConstants.CENTER);
			dropHereLabel.setToolTipText("Drag and Drop Files or Folders Here");
			dropHereLabel.setBackground(Color.WHITE);
			dropHereLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
			dropHereLabel.setOpaque(true);
			dropTarget = new DropTarget(getLabel(), this);
		}
		return dropHereLabel;
	}

	// drop target listener events

	public void dragEnter(DropTargetDragEvent dtde)
	{
		getLabel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void dragExit(DropTargetEvent dte)
	{
		getLabel().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void dragOver(DropTargetDragEvent dtde)
	{
	}

	// drop( ) method listed below

	public void dropActionChanged(DropTargetDragEvent dtde)
	{
	}

	public void drop(DropTargetDropEvent dtde)
	{
		dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		Transferable trans = dtde.getTransferable();
		boolean gotData = false;
		try
		{
			// try to get an image
			if (trans.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				java.util.List list = (java.util.List) trans.getTransferData(DataFlavor.javaFileListFlavor);
				ListIterator it = list.listIterator();
				while (it.hasNext())
				{
					File f = (File) it.next();
					addUpload(0, f.getAbsolutePath());
				}
				gotData = true;
			}
			else if (trans.isDataFlavorSupported(uriListFlavor))
			{
				String uris = (String) trans.getTransferData(uriListFlavor);

				// url-lists are defined by rfc 2483 as crlf-delimited
				StringTokenizer izer = new StringTokenizer(uris, "\r\n");
				while (izer.hasMoreTokens())
				{
					String uristring = izer.nextToken();
					URI parsed = new URI(uristring);
					String path = parsed.getPath();
					addUpload(0, path);
				}
				gotData = true;
			}
			else if (trans.isDataFlavorSupported(urlFlavor))
			{
				URL url = (URL) trans.getTransferData(urlFlavor);
				addUpload(0, url.getPath()); //TODO: Are these local?
				gotData = true;
			}
		}
		catch (Exception e)
		{
			handleError(e);
		}
		finally
		{
			dtde.dropComplete(gotData);
		}
	}

	private void handleError(Exception inE)
	{
		JOptionPane.showMessageDialog(this, "Error " + inE);

	}

	public File getLastSelectedDirectory()
	{
		return fieldLastSelectedDirectory;
	}

	public void setLastSelectedDirectory(File inLastSelectedDirectory)
	{
		fieldLastSelectedDirectory = inLastSelectedDirectory;
	}

	public JSObject getJSObject()
	{
		if (fieldJSO == null)
		{
			fieldJSO = JSObject.getWindow(this);
		}

		return fieldJSO;
	}

	public void sentBytes(final Upload inUpload)
	{
		long now = System.currentTimeMillis();
		if( now < (fieldLastSent + 1000) ) //only update UI once a second
		{
			return;
		}
		fieldLastSent = now;
		final String[] args = createLog(inUpload);
		//			if( time == 0)
		//			{
		//				time = 1;
		//			}
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				if (!inUpload.isFinished() && isInBrowser())
				{
					try
					{
						getJSObject().call("uploadstatus", args);
					}
					catch( JSException e )
					{
						System.out.println("Error while updating file status.");
						
						System.out.print("\ttried to call: uploadstatus( ");
						for(int i = 0; i < args.length; i++) System.out.print(args[i] + " ");
						System.out.println(")");
						
						e.printStackTrace();
					}
				}
				relabel();
			}
		};
		getUiTimer().schedule(task, 0);
	}

	protected String[] createLog(final Upload inUpload)
	{
		double time = (System.currentTimeMillis() - inUpload.getStartSendingTime()) / 1000D;
		if (time == 0)
		{
			time = 1;
		}

		final String[] args = getFixedArgs(5);
		args[0] = String.valueOf(inUpload.getCount());
		args[1] = inUpload.toFullSavePath();
		args[2] = String.valueOf(inUpload.getSentSoFar());
		args[3] = String.valueOf(inUpload.getTotalSize());

		double bitespersec = inUpload.getSentSoFar() / time;
		String speed = getEnglish().inEnglish(new Double(bitespersec)) + "/s";

		//time remaining
		double left = inUpload.getTotalSize() - inUpload.getSentSoFar();
		if(bitespersec == 0)
		{
			bitespersec = 1;
		}
		double secondsremain = left / bitespersec;

		String english = getEnglish().inEnglishTime(secondsremain);
		String done = english + " remaining at " + speed;
		args[4] = done;
		return args;
	}

	public boolean isInBrowser()
	{
		if (fieldInBrowser == null)
		{
			try
			{
				getJSObject();
				fieldInBrowser = Boolean.TRUE;
			}
			catch (JSException ex)
			{
				fieldInBrowser = Boolean.FALSE;
				System.out.println("Not in browser " + ex);
			}
		}
		return fieldInBrowser.booleanValue();
	}

	public void fileFinished(Upload inUpload)
	{
		final String[] args = getFixedArgs(3);
		args[0] = String.valueOf(inUpload.getCount());
		args[1] = inUpload.toFullSavePath();
		final String[] log = createLog(inUpload);
		System.out.println("Full Save Path: " + inUpload.toFullSavePath());

		
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				if (isInBrowser())
				{
					try
					{
						getJSObject().call("uploadstatus", log);
						getJSObject().call("uploadfinished", args);
					}
					catch( JSException e )
					{
						System.out.println("Error while finishing file:");
						
						System.out.print("\ttried to call: uploadstatus( ");
						for(int i = 0; i < log.length; i++) System.out.print(log[i] + " ");
						System.out.println(")");
						
						System.out.print("\ttried to call: uploadfinished( ");
						for(int i = 0; i < args.length; i++) System.out.print(args[i] + " ");
						System.out.println(")");
						
						e.printStackTrace();
					}
				}
				relabel();
			}
		};
		getUiTimer().schedule(task, 0);
	}

	public String[] getFixedArgs(int inAdditionalArgs)
	{
		String[] args;
		String count = getParameter("count");
		if (count != null)
		{
			args = new String[inAdditionalArgs + 1];
			args[inAdditionalArgs] = count;
		}
		else
		{
			args = new String[inAdditionalArgs];
		}
		return args;
	}

	public HttpUploader getMultiPartUpload()
	{
		if (fieldMultiPartUpload == null)
		{
			fieldMultiPartUpload = new HttpUploader();
			fieldMultiPartUpload.addProgressListener(this);

		}
		return fieldMultiPartUpload;
	}

	public void setMultiPartUpload(HttpUploader inMultiPartUpload)
	{
		fieldMultiPartUpload = inMultiPartUpload;
	}

	public List getUploads()
	{
		if (fieldUploads == null)
		{
			fieldUploads = new ArrayList();
		}
		return fieldUploads;
	}

	public void setUploads(List inUploads)
	{
		fieldUploads = inUploads;
	}

	public English getEnglish()
	{
		if (fieldEnglish == null)
		{
			fieldEnglish = new English();
		}
		return fieldEnglish;
	}

	public void setEnglish(English inEnglish)
	{
		fieldEnglish = inEnglish;
	}

	public Timer getTimer()
	{
		if (fieldTimer == null)
		{
			fieldTimer = new Timer(true);
		}
		return fieldTimer;
	}

	public Timer getUiTimer()
	{
		if (fieldUiTimer == null)
		{
			fieldUiTimer = new Timer(true);
		}
		return fieldUiTimer;
	}

	public void setTimer(Timer inTimer)
	{
		fieldTimer = inTimer;
	}
	public CopyFileUploader getCopyFileUploader()
	{
		if (fieldCopyFileUploader == null)
		{
			fieldCopyFileUploader = new CopyFileUploader();
			fieldCopyFileUploader.addProgressListener(this);
		}
		return fieldCopyFileUploader;
	}

	public void setCopyFileUploader(CopyFileUploader inCopyFileUploader)
	{
		fieldCopyFileUploader = inCopyFileUploader;
	}
	public void stop()
	{
		super.stop();
		if( fieldMultiPartUpload != null)
		{
			fieldMultiPartUpload.setRun(false);
		}
		if( fieldCopyFileUploader != null)
		{
			fieldCopyFileUploader.setRun(false);
		}
	}
}
