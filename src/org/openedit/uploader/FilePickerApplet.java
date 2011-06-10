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
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;


import netscape.javascript.JSException;
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
	protected boolean fieldMultiFile = true;
	
	public void start()
	{
		super.start();
    	System.out.println("Picker loaded version " + getClass().getPackage().getImplementationVersion() );

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
		
		String multi = getParameter("multiselect");
		if (multi != null && !Boolean.parseBoolean(multi))
		{
			fieldMultiFile = false;
		}
		
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
    		getSelectButton().doClick();
    	}
    }

	protected JButton getSelectButton()
	{
		if( fieldSelectButton == null)
		{
			fieldSelectButton = new JButton("Select a file...");
			fieldSelectButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JFileChooser chooser = new JFileChooser();
					chooser.setFileHidingEnabled(false);
					chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					chooser.setMultiSelectionEnabled(false);
					chooser.setDialogType(JFileChooser.OPEN_DIALOG);
					chooser.setDialogTitle("Choose a file...");
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
					if( getLastSelectedDirectory() != null)
					{
						chooser.setSelectedFile(getLastSelectedDirectory());
					}
					
					if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
					{
						File targetFile = chooser.getSelectedFile();
						addAllFiles(targetFile);
					}
				}
			});
		}
		return fieldSelectButton;
	}
		
	protected void addUploadPath(String inAbsolutePath)
	{
		String[] args = getDefaultArguments(1);
		args[0] = inAbsolutePath;
		try
		{
			getJSObject().call("setUploadPath", args);
		}
		catch( JSException e )
		{
			System.out.println("Error while adding new file.");
			e.printStackTrace();
		}
	}

	protected String[] getDefaultArguments(int inAdditional)
	{
		String count = getParameter("count");
		String[] args;
		if (count != null)
		{
			args = new String[inAdditional + 1];
			args[1] = count;
		}
		else
		{
			args = new String[inAdditional];
		}
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
					addAllFiles(f);
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
					addAllFiles(path);
				}
				gotData = true;
			}
			else if (trans.isDataFlavorSupported(urlFlavor))
			{
				URL url = (URL) trans.getTransferData(urlFlavor);
				String path = url.getPath();
				addAllFiles(path);
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

	protected void addAllFiles(File inFile)
	{
		if (inFile.isDirectory())
		{
			File[] child = inFile.listFiles();
			for (int i = 0; i < child.length; i++)
			{
				addAllFiles(child[i]);
			}
		}
		else
		{
			addUploadPath(inFile.getAbsolutePath());
			JLabel label = getLabel();
			if (isMultiSelect())
			{
				if(label.getText().indexOf(";") < 0)
				{
					label.setText("");
				}
				label.setText(label.getText() + inFile.getName() + ";");
				label.setToolTipText(label.getText());
			}
			else
			{
				label.setText(inFile.getAbsolutePath());
				label.setToolTipText(label.getText());
			}
		}
	}
	
	protected void addAllFiles(String inPath)
	{
		File f = new File(inPath);
		addAllFiles(f);
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
	
	public boolean isMultiSelect()
	{
		return fieldMultiFile;
	}
}
