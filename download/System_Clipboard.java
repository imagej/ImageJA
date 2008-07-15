import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import ij.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.gui.*;
	
/**	Copies and Pastes images to the system clipboard. Requires 1.4 or later. 
	The code for converting pasted PICTs to AWT images on Mac OS X  
	using QuickTime for Java was contributed by Gord Peters. */
public class System_Clipboard extends PlugInFrame implements ActionListener, Transferable {
 	Button copyButton;
	Button pasteButton;
	Clipboard clipboard; 
	static Frame instance;
        ImageJ ij;

	public System_Clipboard() {
		super("Clipboard");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		WindowManager.addWindow(this);
		if (!IJ.isJava14()) {
			IJ.error ("This plugin requires Java 1.4 or later");
			return;
		}
		ij = IJ.getInstance();
		addKeyListener(ij);
		copyButton = new Button("Copy");
		pasteButton = new Button("Paste");
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		clipboard = toolkit.getSystemClipboard();
		copyButton.addActionListener(this);
		copyButton.addKeyListener(ij);
		pasteButton.addActionListener(this);
		pasteButton.addKeyListener(ij);
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout());
		panel.add(copyButton);
		panel.add(pasteButton);
		add(panel);
		pack();
		GUI.center(this);
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		try {
  			if (e.getSource() == copyButton)
				clipboard.setContents(this, null);
			else {
				Transferable transferable = clipboard.getContents(null);
				if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
					Image img = (Image)transferable.getTransferData(DataFlavor.imageFlavor);
					new ImagePlus("Pasted Image", img).show();
				} else if (IJ.isMacOSX()) {
					Image img = getMacImage(transferable);
					if (img!=null)
						new ImagePlus("Pasted Image", img).show();
				} else
					IJ.showMessage("System_Clipboard", "No image data in the system clipboard.");
			}
		} catch (Throwable t) {}
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.imageFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.imageFlavor.equals(flavor);
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (!isDataFlavorSupported(flavor))
			throw new UnsupportedFlavorException(flavor);
		ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp != null) {
			ImageProcessor ip = imp.getProcessor();
			ip = ip.crop();
			int w = ip.getWidth();
			int h = ip.getHeight();
			IJ.showStatus(w+"x"+h+ " image copied to system clipboard");
			Image img = createImage(w, h);
			Graphics g = img.getGraphics();
			g.drawImage(ip.createImage(), 0, 0, null);
			g.dispose();
			return img;
		} else {
			//IJ.noImage();
			return null;
		}
	}

	// Mac OS X's data transfer handling is horribly broken... we
	// need to use the "image/x-pict" MIME type and then Quicktime
	// for Java in order to obtain image data without problems.
	Image getMacImage(Transferable t) {
		if (!isQTJavaInstalled()) {
			IJ.error("QuickTime for Java is not installed");
			return null;
		}
		Image img = null;
IJ.log("getMacImage: "+t); IJ.wait(2000);
		DataFlavor[] d = t.getTransferDataFlavors();
		if (d==null || d.length==0)
			return null;
		//IJ.log(d[0]+": "+d[0]); IJ.wait(2000);
		try {
			Object is = t.getTransferData(d[0]);
			if (is==null || !(is instanceof InputStream)) {
				IJ.beep();
				IJ.showStatus("Clipboad does not appear to contain an image");
				return null;
			}
			img = getImageFromPictStream((InputStream)is);
		} catch (Exception e) {}
		return img;
    }

	// Converts a PICT to an AWT image using QuickTime for Java.
	// This code was contributed by Gord Peters.
	Image getImageFromPictStream(InputStream is) {
		try {
			ByteArrayOutputStream baos= new ByteArrayOutputStream();
			// We need to strip the header from the data because a PICT file
			// has a 512 byte header and then the data, but in our case we only
			// need the data. --GP
			byte[] header= new byte[512];
			byte[] buf= new byte[4096];
			int retval= 0, size= 0;
			baos.write(header, 0, 512);
			while ( (retval= is.read(buf, 0, 4096)) > 0)
				baos.write(buf, 0, retval);		
			baos.close();
			size = baos.size();
			//IJ.log("size: "+size); IJ.wait(2000);
			if (size<=0)
				return null;
			byte[] imgBytes= baos.toByteArray();
			// Again with the uglyness.  Here we need to use the Quicktime
			// for Java code in order to create an Image object from
			// the PICT data we received on the clipboard.  However, in
			// order to get this to compile on other platforms, we use
			// the Java reflection API.
			//
			// For reference, here is the equivalent code without
			// reflection:
			//
			//
			// if (QTSession.isInitialized() == false) {
			//     QTSession.open();
			// }
			// QTHandle handle= new QTHandle(imgBytes);
			// GraphicsImporter gi=
			//     new GraphicsImporter(QTUtils.toOSType("PICT"));
			// gi.setDataHandle(handle);
			// QDRect qdRect= gi.getNaturalBounds();
			// GraphicsImporterDrawer gid= new GraphicsImporterDrawer(gi);
			// QTImageProducer qip= new QTImageProducer(gid,
			//                          new Dimension(qdRect.getWidth(),
			//                                        qdRect.getHeight()));
			// return(Toolkit.getDefaultToolkit().createImage(qip));
			//
			// --GP
			//IJ.log("quicktime.QTSession");
			Class c = Class.forName("quicktime.QTSession");
			Method m = c.getMethod("isInitialized", null);
			Boolean b= (Boolean)m.invoke(null, null);			
			if (b.booleanValue() == false) {
				m= c.getMethod("open", null);
				m.invoke(null, null);
			}
			c= Class.forName("quicktime.util.QTHandle");
			Constructor con = c.getConstructor(new Class[] {imgBytes.getClass() });
			Object handle= con.newInstance(new Object[] { imgBytes });
			String s= new String("PICT");
			c = Class.forName("quicktime.util.QTUtils");
			m = c.getMethod("toOSType", new Class[] { s.getClass() });
			Integer type= (Integer)m.invoke(null, new Object[] { s });
			c = Class.forName("quicktime.std.image.GraphicsImporter");
			con = c.getConstructor(new Class[] { type.TYPE });
			Object importer= con.newInstance(new Object[] { type });
			m = c.getMethod("setDataHandle",
			new Class[] { Class.forName("quicktime.util." + "QTHandleRef") });
			m.invoke(importer, new Object[] { handle });
			m = c.getMethod("getNaturalBounds", null);
			Object rect= m.invoke(importer, null);
			c = Class.forName("quicktime.app.view.GraphicsImporterDrawer");
			con = c.getConstructor(new Class[] { importer.getClass() });
			Object iDrawer = con.newInstance(new Object[] { importer });
			m = rect.getClass().getMethod("getWidth", null);
			Integer width= (Integer)m.invoke(rect, null);
			m = rect.getClass().getMethod("getHeight", null);
			Integer height= (Integer)m.invoke(rect, null);
			Dimension d= new Dimension(width.intValue(), height.intValue());
			c = Class.forName("quicktime.app.view.QTImageProducer");
			con = c.getConstructor(new Class[] { iDrawer.getClass(), d.getClass() });
			Object producer= con.newInstance(new Object[] { iDrawer, d });
			if (producer instanceof ImageProducer)
				return(Toolkit.getDefaultToolkit().createImage((ImageProducer)producer));
		} catch (Exception e) {IJ.showStatus("QuickTime for java error");}
		return null;
    }

	// Retuns true if QuickTime for Java is installed.
	// This code was contributed by Gord Peters.
	boolean isQTJavaInstalled() {
		boolean isInstalled = false;
		try {
			Class c= Class.forName("quicktime.QTSession");
			isInstalled = true;
		} catch (Exception e) {}
		return isInstalled;
	}

	public Insets getInsets() {
		Insets i= super.getInsets();
		return new Insets(i.top+5, i.left+40, i.bottom+5, i.right+40);
	}
	
	public void close() {
		super.close();
		instance = null;	
	}

}



