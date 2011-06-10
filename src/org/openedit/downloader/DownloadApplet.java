package org.openedit.downloader;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import netscape.javascript.JSObject;

import org.openedit.uploader.English;

public class DownloadApplet extends JApplet
{
	private static final long serialVersionUID = 1L;
	protected Timer fieldTimer;
	protected JButton fieldSelectButton;
	protected File fieldLastSelectedDirectory;
	protected List fieldDownloads;
	protected JSObject fieldJSO;
	protected English fieldEnglish;

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
			e.printStackTrace();
		}
		setLayout(new GridBagLayout());
		if( getParameter("downloadsite") == null )
		{
			setContentPane(new JLabel("downloadsite, downloadcount and path1 path2 etc.. is required"));
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

	    GridBagConstraints b = (GridBagConstraints)c.clone();
	    b.weightx = 1;
		//add(getLabel(), b);
	    add( getSelectButton(), b );
		
	}

	public void startDownloads(File inFolder)
	{
		String downloadsite = getParameter("downloadsite");
		String downloadcount = getParameter("downloadcount");
		String cookies = getParameter("cookies"); 
		if( downloadcount != null)
		{

			File[] children = inFolder.listFiles();
			if( children != null && children.length > 0)
			{
				String text = "The selected folder contains " + children.length +
					" files.\nPress OK to delete them and download your album.\nClick" +
					" 'No' to download without deleting existing files, which might be overwritten." +
					"\nClick 'Cancel' if you don't want to download this album.";
				int val = JOptionPane.showConfirmDialog(this, text);
				if( val == JOptionPane.OK_OPTION)
				{
					for (int j = 0; j < children.length; j++)
					{
						File child = children[j];
						child.delete(); //does not delete folders
					}
				}
				else if (val == JOptionPane.CANCEL_OPTION)
				{
					return;
				}
			}
			
			// disable download button
			getSelectButton().setEnabled(false);
			String text = getSelectButton().getText();
			getSelectButton().setText("Downloading...");
			
			int count = Integer.parseInt(downloadcount);
			for (int i = 0; i < count; i++)
			{
				String path = getParameter("path" + i);
				//addDownload(downloadsite, path);
				//we have a file to download into this folder
				File name = new File(path);
				File outfile = new File(inFolder, name.getName());
				if (outfile.exists() && outfile.length() > 0)
				{
					String message = "File '" + name.getName() + "' already exists. Do you want to overwrite it?";
					int input = JOptionPane.showConfirmDialog(this, message, "Overwrite File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (input == JOptionPane.NO_OPTION)
					{
						continue;
					}
				}
				try
				{
					URL fromserver = new URL(downloadsite + path.replaceAll(" ", "%20"));

					URLConnection con = fromserver.openConnection();
					con.setRequestProperty("Cookie", cookies);

					InputStream in = con.getInputStream();
					
					FileOutputStream out = new FileOutputStream(outfile);
					byte[] bytes = new byte[1024];

					int iRead = -1;
					long startSendingTime = System.currentTimeMillis();
					int sofar = 0;
					int total = con.getContentLength();
					while (true)
					{
						iRead = in.read(bytes);

						if (iRead != -1)
						{
							out.write(bytes, 0, iRead);
							if( total > 0)
							{
								sentBytes(i,startSendingTime,path,sofar,total);
							}
						}
						else
						{
							break;
						}
					}
					out.close();
					in.close();																																																																																																																																
					
				}																																																																																																																																																								
				catch ( Exception ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this,ex.toString());
				}
				
			}
			//Re-enable button
			getSelectButton().setText(text);
			getSelectButton().setEnabled(true);
		}
		JOptionPane.showMessageDialog(this,"Export is complete");
	}
	
	public Timer getTimer()
	{
		if (fieldTimer == null)
		{
			fieldTimer = new Timer(true);
		}
		return fieldTimer;
	}


	public void setTimer(Timer inTimer)
	{
		fieldTimer = inTimer;
	}

	protected JButton getSelectButton()
	{
		if( fieldSelectButton == null)
		{
			fieldSelectButton = new JButton("Choose a directory...");
			fieldSelectButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JFileChooser chooser = new JFileChooser();
					chooser.setFileHidingEnabled(false);
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setMultiSelectionEnabled(false);
					chooser.setDialogType(JFileChooser.OPEN_DIALOG);
					chooser.setDialogTitle("Choose a directory...");
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
					}
					);
					//chooser.setSelectedFile(file);
					if( getLastSelectedDirectory() != null)
					{
						chooser.setSelectedFile(getLastSelectedDirectory());
					}
					
					if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
					{
						File targetFolder = chooser.getSelectedFile();
						startDownloads(targetFolder);
						//tfdTargetFile.setText(targetFile.toString());
						//btnDoUpload.setEnabled(true);					
					}
				}
			});
		}
		return fieldSelectButton;
	}

	public File getLastSelectedDirectory()
	{
		return fieldLastSelectedDirectory;
	}

	public void setLastSelectedDirectory(File inLastSelectedDirectory)
	{
		fieldLastSelectedDirectory = inLastSelectedDirectory;
	}

	public List getDownloads()
	{
		if (fieldDownloads == null)
		{
			fieldDownloads = new ArrayList();
			
		}

		return fieldDownloads;
	}

	public void setDownloads(List inDownloads)
	{
		fieldDownloads = inDownloads;
	}

	public JSObject getJSObject()
	{
		if (fieldJSO == null)
		{
			fieldJSO = JSObject.getWindow(this);
		}

		return fieldJSO;
	}

	public void setJSO(JSObject inJso)
	{
		fieldJSO = inJso;
	}

	public void sentBytes(int count, long startSendingTime,  String inPath, int sentSoFar, int inTotal)
	{
		String[] args = new String[5];
		args[0] = String.valueOf( count );
		args[1] = inPath;
		args[2] = String.valueOf( sentSoFar );
		args[3] = String.valueOf( inTotal);
		
		double time = (System.currentTimeMillis() - startSendingTime ) / 1000D;
		if( time == 0)
		{
			time = 1;
		}
		double bitespersec =  sentSoFar / time;
		String speed = getEnglish().inEnglish(new Double(bitespersec))  + "/s";
		
		if (sentSoFar == inTotal)
		{
			double took = (System.currentTimeMillis() - startSendingTime ) / 1000D;
			String done=  "uploaded in " + getEnglish().inEnglishTime(took) + " at " + speed;
			args[4] = done;
			getJSObject().call("downloadfinished", args);
		}
		else
		{
			//time remaining
			double left = inTotal - startSendingTime; 
			double secondsremain = left / bitespersec;
			
			String english = getEnglish().inEnglishTime(secondsremain);
			String done = english + " remaining at " + speed;
			args[4] = done;
			getJSObject().call("downloadstatus", args);

		}
		
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
	
}
