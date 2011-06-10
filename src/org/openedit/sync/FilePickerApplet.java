package org.openedit.sync;

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

import netscape.javascript.JSObject;

/**
 * 1. Show progress bar as the data is sent over the wire 
 * 2. Add a count 1 of 23images 
 * 3. Might want to add a paste button if applicable. Also might want to
 * show a browse button with multi-select enabled 
 * 4. Oh and another nice thing... Check that the images are not already uploaded before being sent
 * 5. Add JPG Compression
 * 
 * @author cburkey
 */

public class FilePickerApplet extends JApplet implements DropTargetListener
{
	private static final long serialVersionUID = 1L;
	
	protected DropTarget dropTarget;
	protected JLabel dropHereLabel;
	protected JButton fieldSelectButton;
	protected JSObject fieldJSO;
	
	protected static DataFlavor urlFlavor, uriListFlavor, macPictStreamFlavor;
	protected File fieldLastSelectedDirectory;
	protected List fieldUploads;
	protected JFileChooser fieldChooser;

	public JFileChooser getChooser()
	{
		if (fieldChooser == null)
		{
			fieldChooser = new JFileChooser();
			fieldChooser.setFileHidingEnabled(false);
			fieldChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fieldChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fieldChooser.setMultiSelectionEnabled(true);
			fieldChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fieldChooser.setDialogTitle("Choose a file or directory...");
			fieldChooser.setFileFilter(new FileFilter()
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

		}

		return fieldChooser;
	}

	public void setChooser(JFileChooser inChooser)
	{
		fieldChooser = inChooser;
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

	public void start()
	{
		super.start();
		Color bg = Color.WHITE;
		String bgcolor = getParameter("bgcolor");
		if (bgcolor != null && bgcolor.length() == 6)
		{
			String sr = bgcolor.substring(0,2);
			String sg = bgcolor.substring(2, 4);
			String sb = bgcolor.substring(4,6);
			int r = Integer.parseInt(sr, 16);
			int g = Integer.parseInt(sg, 16);
			int b = Integer.parseInt(sb, 16);
			bg = new Color(r,g,b);
		}
		getContentPane().setBackground(bg);
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
			uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
    	setLayout(new GridBagLayout());
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
		add(getLabel(), b);

		b = (GridBagConstraints)c.clone();
	    b.gridx = 1;
	    b.fill = GridBagConstraints.NONE;
	    b.anchor = GridBagConstraints.NORTH;
	    
    	add( getSelectButton(), b );
    	
    	String auto = getParameter("showdialog");
    	if( "true".equals(auto) )
    	{
    		SwingUtilities.invokeLater( new Runnable()
			{
				public void run()
				{
					getSelectButton().doClick();
				}
			});
    	}
    }

	protected JButton getSelectButton()
	{
		if( fieldSelectButton == null)
		{
			fieldSelectButton = new JButton("Select files or folder...");
			fieldSelectButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JFileChooser chooser = getChooser();
					if( getLastSelectedDirectory() != null)
					{
						chooser.setSelectedFile(getLastSelectedDirectory());
					}
					
					if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
					{
						File[] targetFile = chooser.getSelectedFiles();
						getUploads().clear();
						for (int i = 0; i < targetFile.length; i++)
						{
							getUploads().add(targetFile[i]);	
							setLastSelectedDirectory(targetFile[i]);
						}
						updateBrowser();
					}
				}
			});
		}
		return fieldSelectButton;
	}
	protected void addUpload(File inAbsolutePath)
	{
		getUploads().add(inAbsolutePath);		
		updateBrowser();
	}
	protected void updateBrowser()
	{
		try
		{
			relabelFiles();
	
			StringBuffer paths = new StringBuffer();
			StringBuffer parents = new StringBuffer();
			StringBuffer sizes = new StringBuffer();
			for (int i = 0; i < getUploads().size(); i++)
			{
				File file = (File) getUploads().get(i);
				//folder check here
				processFolders(paths, parents, sizes, null,file);
			}
			
			String[] args = new String[3];
			args[0] = trim(paths.toString());
			args[1] = trim(parents.toString());			
			args[2] = trim(sizes.toString());
			//set old data
			getJSObject().call("setUploads", args);
			//log(args.toString());
		}
		catch( Exception e )
		{
			handleError(e);
		}
	}

	private String trim(String inString)
	{
		if( inString != null && inString.endsWith(";"))
		{
			inString = inString.substring(0, inString.length() - 1);
		}
		return inString;
	}

	private void processFolders(StringBuffer paths, StringBuffer parents, StringBuffer sizes,String inParent, File file)
	{
		if( file.isDirectory())
		{
			if( inParent == null)
			{
				inParent = "";
			}
			inParent = inParent + "/" + file.getName();
			
			File[] children = file.listFiles();
			for (int j = 0; j < children.length; j++)
			{
				File child = children[j];
				String name = child.getName();
				if( name.startsWith(".") || name.endsWith(".db"))
				{
					continue;
				}
				processFolders(paths,parents, sizes, inParent, child);						
			}
		}
		else
		{
			addFile(paths, parents, sizes, inParent, file);					
		}
	}

	private void addFile(StringBuffer paths, StringBuffer parents, StringBuffer sizes, String inParentPath, File file)
	{
		if( inParentPath == null)
		{
			inParentPath = "/" + file.getParentFile().getName();
		}
		parents.append(inParentPath);
		parents.append(";");
		paths.append(file.getAbsolutePath());
		paths.append(";");
		sizes.append(file.length());
		sizes.append(";");
	}

	private void log(String inLog)
	{
		System.out.println(inLog);
	
	}

	private void relabelFiles()
	{
		JLabel label = getLabel();
		StringBuffer data = new StringBuffer();
		for (Iterator iterator = getUploads().iterator(); iterator.hasNext();)
		{
			File upload = (File) iterator.next();
			data.append( upload.getName() );
			if( upload.isDirectory())
			{
				int c = countFiles(upload);
				data.append("/(" + c + " files)"); //Add count?
			}
			data.append("; ");
			if( data.length() > 200)
			{
				data.append("... " + getUploads().size() + " files" );
				break;
			}
		}
		label.setText(data.toString());
		label.setToolTipText(getUploads().size() + " total files");
	}

	private int countFiles(File inUpload)
	{
		if( inUpload.isFile() )
		{
			return 1;
		}
		File[] children = inUpload.listFiles();
		int c = 0;
		if( children != null)
		{
			for (int i = 0; i < children.length; i++)
			{
				File child = children[i];
				c = c + countFiles(child);
			}
		}
		return c;
	}

	protected String[] createNewMessage(int inRow, int inProperties)
	{
		String[] args = new String[inProperties + 1];
		args[0] = String.valueOf(inRow);
		
		return args;
	}
	
	private JLabel getLabel()
	{
		if( dropHereLabel == null)
		{
			//Todo add a paste button
			dropHereLabel = new JLabel("", SwingConstants.CENTER);
			dropHereLabel.setToolTipText("Drag and Drop Files or Folders Here");
			dropHereLabel.setText(dropHereLabel.getToolTipText());
			dropHereLabel.setBackground(Color.WHITE);
			dropHereLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
			dropHereLabel.setOpaque(true);
			dropTarget = new DropTarget(dropHereLabel, this);
		}
		return dropHereLabel;
	}


	// drop target listener events

	public void dragEnter(DropTargetDragEvent dtde)
	{
		getLabel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR ));
	}

	public void dragExit(DropTargetEvent dte)
	{
		getLabel().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR ));
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
				List list = (List) trans.getTransferData(DataFlavor.javaFileListFlavor);
				ListIterator it = list.listIterator();
				while (it.hasNext())
				{
					File f = (File) it.next();
					addUpload(f);
				}
				gotData = true;
			}
			// No URL drop yet
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
					addUpload(new File( path ));
				}
				gotData = true;
			}
			else if (trans.isDataFlavorSupported(urlFlavor))
			{
				URL url = (URL) trans.getTransferData(urlFlavor);
				String path = url.getPath();
				addUpload(new File( path ));
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
		JOptionPane.showMessageDialog(this,"Error " +  inE);
		
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
}
