import java.util.Properties;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import java.lang.reflect.*;

/*
* @version  	1.2  Date 9 July
* 					- various bug fixes
* 					- code refactoring - > helper class created
* 				1.1
* 					- Drag and Drop support
* 				1.0   03 July 2009
* 					- basic URL handling 
* 
*     @author	Dimiter Prodanov
*        		IMEC
*  		"read the source, meditate, get rich, buy a Ferari"
* 
* @contents This plugin installs plugins packaged as jar files from the local file system or URLs
* 			The plugin supports specifically drag and drop from  Mozilla Firefox browser tabs and can parse HTML for valid URLs 
*    
*   User interaction:
* 	q - quits the plugin
* 	? - displays help message
*	
*
* @license      This library is free software; you can redistribute it and/or
*      modify it under the terms of the GNU Lesser General Public
*      License as published by the Free Software Foundation; either
*      version 2.1 of the License, or (at your option) any later version.
*
*      This library is distributed in the hope that it will be useful,
*      but WITHOUT ANY WARRANTY; without even the implied warranty of
*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*       Lesser General Public License for more details.
*
*      You should have received a copy of the GNU Lesser General Public
*      License along with this library; if not, write to the Free Software
*      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/

public class PlugInInstaller_ implements PlugIn, 
										 DropTargetListener, 
										 KeyListener {
	
	private static Properties props;  
	public final static String version="1.2";
	
	public static  String fileSeparator="";
	public static  String pathSeparator="";
	public static  String pluginsDir="";
 
	public String defaultUrl="";

	DropTarget dt;
	boolean proxyset=new Boolean(System.getProperty("proxySet")).booleanValue();

	boolean downloaded=false;
	
	private long timestamp=-1;
	private URL jarURL=null;
	
	private boolean debug=IJ.debugMode;
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
    	try {
    		System.setProperty("plugins.dir", args[0]);
    		//String plugins_dir=props.getProperty("plugins.dir");
    		new ImageJ();
    		//props = System.getProperties();
     		//props.list(System.out);
    	
    	}
    	catch (Exception ex) {
    		IJ.log("plugins.dir misspecified");
    	}

	}
 
	
	
	/*   file.separator: \
  	 *	 path.separator: ;
  	 *   java.ext.dirs: 
  	 *   java.io.tmpdir: 
  	 *   
  	 *  
	 * 
	 * (non-Javadoc)
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg) {
		//if (IJ.getApplet()!=null) return;
		ImageJ ij=IJ.getInstance();
		ij.addKeyListener(this);
		IJ.log("starting ...");
		
		//filehandler.init();
	
		if (showDnDDialog()) { // case Drag and Drop entry
			ij = IJ.getInstance();
			//ij.setDropTarget(null);
			dt = new DropTarget(ij, this);
			 
		 } else {
			 if (showURLDialog()) { // case Dialog entry
				try {
					downloaded=filehandler.getJarFile(jarURL, pluginsDir);
					IJ.log("downloaded : " + downloaded);
					if (downloaded) filehandler.updateMenus();
				} catch (IOException ioe) {
					
					Log("IO Exception");
					ioe.printStackTrace();
					//IJ.error(ioe.toString());
				} 
				 
			}
		}
	
		dt=null;
		 //dispose();
	}

	

	/**
	 * @param filename
	 */
	private boolean checkFile(String filename, boolean deleteDialog) {
		boolean ret=false;
		if ((filename.isEmpty()) || (filename==null))  return false;
		
		//File f=new File(filename);
		ret=checkFile(new File(filename),deleteDialog);
		return ret;
	}
	
	/**
	 * @param afile
	 * 
	 */
	private boolean checkFile(File afile, boolean deleteDialog){
		boolean ret=false;
		if (!afile.exists() ) {
			return true;
		} else if (afile.isFile()) {
			ret=true;
			timestamp=afile.lastModified();
			Log("file exists");
			try {
				if (deleteDialog) {
					
					GenericDialog gd=new GenericDialog("File exists:");  
					gd.addMessage(afile.getCanonicalPath());
					gd.addCheckbox("Delete?", true);
					gd.showDialog();
					
					if (gd.getNextBoolean()) {
						if (afile.canWrite())
							ret=afile.delete();
						else {
							Log("Specified file is not writeable");
							ret=false;
						}
					}
				} // end deleteDialog
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}

		
	private boolean showURLDialog() {
		 //String URLString="http://some.address.net/SomeJar.jar";
		 String aURLaddress=defaultUrl;
		 	GenericDialog gd=new GenericDialog("URL");
		
	        // Dialog box for user input
	        gd.addMessage("This plugin installs plugins packaged as jar files\n");
	        gd.addMessage("type url in the form: http://some.address.net/SomeJar.jar\n");
	        // radius=size/2-1;
	        gd.addStringField("URL", aURLaddress, 64);
	        gd.showDialog(); 
	        
	        if (gd.wasCanceled())
	            return false;
	         
	        aURLaddress= gd.getNextString();
	        
	        try {
	        	 
	        	jarURL=filehandler.formURL(aURLaddress);
	        	
	        	
	        } catch (MalformedURLException e) {
	            IJ.error("Invalid URL: " + aURLaddress);
	            return false;
	        }
	        
	        return true;
	}

	private boolean showDnDDialog() {
		  
		 boolean dndAllow=false; 
		 	GenericDialog gd=new GenericDialog("Plugin Installer "+ version);
		
	        // Dialog box for user input
	        //gd.addMessage("This plugin installs plugins packaged as jar files\n");
	        //gd.addMessage("All\n");
	        // radius=size/2-1;
	        gd.addCheckbox("Configure for Drag and Drop?", dndAllow);
	        gd.addCheckbox("Set debug mode?", debug);
	        gd.showDialog(); 
	        
	        if (gd.wasCanceled())
	            return dndAllow;
	        else { 
	        	dndAllow = gd.getNextBoolean();
	        	debug= gd.getNextBoolean();
	        }
	        
	        
	        return dndAllow;
	}

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void dragExit(DropTargetEvent dtde) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void drop(DropTargetDropEvent dtde) {
			 try {
			      // Ok, get the dropped object and try to figure out what it is
			      Transferable tr = dtde.getTransferable();
			      DataFlavor[] flavors = tr.getTransferDataFlavors();
			      
			      for (int i = 0; i < flavors.length; i++) {
			    	  Log("Possible flavor: " + flavors[i].getMimeType());
			    	  dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			    	  // Check for file lists specifically
					  if (flavors[i].isFlavorJavaFileListType()) {
						 
					    downloaded=filehandler.handleFile( tr, flavors[i]);
					    dtde.dropComplete(downloaded);
						return;
					 }
					  
					// Check for url lists specifically
					  if (flavors[i].isMimeTypeEqual("application/x-java-url") ) {
						 
						  downloaded=filehandler.handleURL(tr, flavors[i]);
						  dtde.dropComplete(downloaded);
						  return;
					  }
					  
					// Check for html code fragments specifically
					  if (flavors[i].isMimeTypeEqual("text/html") ) {
						  downloaded=filehandler.handleHTML( tr, flavors[i]);
						  dtde.dropComplete(downloaded);
							return;
						  
					  }
						  
					  
			  // Ok, is it another Java object?
			  else if (flavors[i].isFlavorSerializedObjectType()) {
				  
				  downloaded=filehandler.handleObject( tr, flavors[i]);
				  dtde.dropComplete(downloaded);
				return;
			  }
			  // How about an input stream?
			  else if (flavors[i].isRepresentationClassInputStream()) {
			     
			    Log("Accepted text drop from system Clipboard");
			    InputStreamReader reader=(InputStreamReader)tr.getTransferData(flavors[i]);
			    Log(filehandler.readInput(reader));
			    
			    dtde.dropComplete(true);
			    return;
			  }
			 }
			      // 
			      Log("Drop failed: " + dtde);
			      dtde.rejectDrop();
			 } catch (Exception e) {
				 
				 IJ.error("Drop failed\n look into the log"); 
				 
				 e.printStackTrace();
				 dtde.rejectDrop();
			 }
			
		}


	


		@Override
		public void dropActionChanged(DropTargetDragEvent arg0) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void keyTyped(KeyEvent e) {
			char c=e.getKeyChar();
			//int code=e.getKeyCode();
			//int modext=e.getModifiersEx();
			//String modexts=e.getModifiersExText(modext);

			switch (c) {
				case 'q':{
		    			dispose();
		    			break;}
				case '?':{
						showHelp(H_QUIT);
						break;}
		 
			}
			e.consume();
			
		}

		public void dispose(){
			Log("disposing of listeners ...");
			ImageJ ij=IJ.getInstance();
	        ij.removeKeyListener(this);
	
		   
		    
		    
		    dt.removeDropTargetListener(this);
	  	}
		
		 private static final int   H_QUIT=1, H_OLDVERSION=2;
		 /**
	     * @param code
	     */
	    void showHelp(int code) {
	     	String msg="";
	      	switch (code) {
    		case H_OLDVERSION:{ 
    			msg="Restart ImageJ";
    			break;
    		}
	    		 
	    		case H_QUIT:{ 
	    			msg="Press q for quit; or drag and drop jar url onto the GUI frame";
	    			break;
	    		}
	    	}
	    	
	        IJ.showMessage("Help Plugin Installer v. " + version,
	         msg
	        );
	        
	    } /* showHelp */
	    
	   public void Log(String s) {
		   if ((IJ.debugMode) || debug) {
			   IJ.log(s);
		   }
	   }
 //////////////////////////////////////////////////////
	    private Handler filehandler=new Handler();
	    
	    private class Handler {
	    	
	    	private String proxyHost;//= System.getProperty("http.proxyHost");
	    	private int proxyPort=-1;//=new Integer(System.getProperty("http.proxyPort")).intValue() ;
	    	private String proxyUser;//=System.getProperty("http.proxyUser");
	    	private String proxyPass;//=System.getProperty("http.proxyPassword");
	    	
	    	public Handler() {
	    		init(proxyset);
	    	}
	    	
	    	/**
	    	 * 
	    	 */
	    	private void init(boolean isProxySet) {

	    		props = System.getProperties();
	    		 
	    		fileSeparator=File.separator; //props.getProperty("file.separator");
	    		pathSeparator= File.pathSeparator; //props.getProperty("path.separator");
	    		pluginsDir=props.getProperty("plugins.dir");
	    		//IJ.log("plugins directory: " +plugins_dir);
	    		
	    		//if (plugins_dir.equalsIgnoreCase(file_separator)) {
	    		if (pluginsDir==null) {
	    			 String homedir= Prefs.getHomeDir();
	    			 Log("ImageJ directory: "+homedir);
	    			 // IJ.log(path_separator);
	    			 // IJ.log(file_separator);
	    			 pluginsDir=homedir+fileSeparator+"plugins";
	    			 Log("plugins directory: " +pluginsDir);
	    		}
	    		pluginsDir+=fileSeparator;
	    		
	    		if (isProxySet) {
	    			String proxyhost= props.getProperty("http.proxyHost");
	    			proxyPort=new Integer(props.getProperty("http.proxyPort")).intValue() ;
	    			proxyUser=props.getProperty("http.proxyUser");
	    			String proxypass=props.getProperty("http.proxyPassword");
	    			Log("proxy set: "+ props.getProperty("proxySet"));
	    			Log("proxy host: "+ props.getProperty("http.proxyHost"));
	    			Log("proxy port: "+props.getProperty("http.proxyPort"));

	    			
	    		}
	    		 
	    	}

	    	
	    	private boolean copyJarFile(File sourceFile, String pluginsDir) throws IOException {
	    		boolean ret=false;
	    		int buffsize=4096;
	    		String source=sourceFile.getCanonicalPath();
	    		Log("source: " +source);
	    		String dest=pluginsDir+ sourceFile.getName();
	    		File destFile=new File(dest);
	    		
	    		dest=destFile.getCanonicalPath();
	    		
	    		Log("destination: " +dest);
	    		
	    		if (dest.equals(source)) {
	    			Log("source and destination are the same!");
	    			return false;
	    		}
	    		
	    		if ( checkFile(sourceFile, false)) {
	    			checkFile(destFile, true);
	    			FileInputStream fis =new FileInputStream(sourceFile);
	    			byte [] buffer=new byte[buffsize];
	    			
	    		
	    			BufferedInputStream  bis=new BufferedInputStream(fis, buffsize);
	    			
	    		 	long fsize=downloadJarFile(buffer, bis, dest);
	    		 	ret=(fsize>=sourceFile.length());
	    		 	
	    			bis.close();
	    			 
	    			fis.close();
	       	   }
	    		
	    	
	    		
	    		return ret;
	    	}
	    	
	    	
	    	/**
	    	 * @param URL
	    	 * @param pluginsDir
	    	 * @throws IOException
	    	 * @throws ConnectException
	    	 */
	    	private boolean getJarFile(URL  url, String pluginsDir) 
	    					throws IOException, ConnectException {
	    		int buffsize=2048;
	    		boolean ret=false;
	    		if (jarURL==null) {
	    			Log("no url provided");
	    			return false;
	    		}
	    		
	    		
	    		   
				Log( "we are getting : " + jarURL.toString());
		
	    		URLConnection uc = url.openConnection();
	    		
				Log("opening URL connection ...");
	    		long len = uc.getContentLength();
	    		// we are waiting for 5s
	    		uc.setReadTimeout(5000);
	    		 Log("Content length: "+len);
	    		 
	    		String ContentType=uc.getContentType();
	    		Log("Content type: "+  ContentType );
	    		try {
		    		//String astr=jarURL.getFile();
		    		if (ContentType.equalsIgnoreCase("application/x-java-archive")) {
		    			String retStr = extractJarName(jarURL);
		    			String filename=pluginsDir+retStr;
		    			
		    			if (checkFile(filename, true)) {
		    	   			InputStream in = uc.getInputStream();
			       			byte [] buffer=new byte[buffsize];
			    			BufferedInputStream  bis=new BufferedInputStream(in, buffsize);
			    			
			    			long fsize= downloadJarFile(buffer, bis, filename);
			    			ret=(fsize>=len);
			    			bis.close();
			    			 
			    			in.close();
		    			}
		    		}
	    		} catch (NullPointerException ex) {
	    			Log("possible request timeout");
	    			throw new ConnectException();
	    		}
	    	
	    		return ret;
	    	}
	    	
	    	public static final int zipsignature=0x504B;
	    	
	    	/**
	    	 * @param buffer
	    	 * @param bis
	    	 * @param filename
	    	 * @throws IOException
	    	 * @throws FileNotFoundException
	    	 */
	    	private long downloadJarFile(byte[] buffer, BufferedInputStream bis,
	    			String filename) throws IOException, FileNotFoundException {
	    		//boolean ret=false;
	    		long fsize=-1;
	    		int bytesRead = -1;
	    		//int step=buffer.length;
	    		bytesRead = bis.read(buffer);
	    		
	    		int tr=(buffer[0] & 0xFF) << 8 | (buffer[1] & 0xFF);
	    		//IJ.log(Integer.toHexString(buffer[0] & 0xFF) + Integer.toHexString(buffer[1] & 0xFF));
	    		Log("file header: " +Integer.toHexString(tr) );
	    		boolean isJar=(tr==zipsignature);
	    		
	    		File f=new File(filename);
	    		boolean isCreated=true;
	    		if (!f.exists()) {
	    			  isCreated = f.createNewFile();
	    		}
	    		if (isJar && isCreated ) {
	    			FileOutputStream fos= new FileOutputStream(filename); 
	    		
	    			fos.write(buffer);
	    			
	    			while ((bytesRead = bis.read(buffer)) != -1) {
	    				fos.write(buffer);
	    				//step+=buffer.length;
	    			}

	    			fos.close();
	    			//File f=new File(filename);
	    				
	    			if (f.exists()) {
	    				//ret=true;
	    				fsize=f.length();
	    				if (timestamp!=f.lastModified()) 
	    				Log("file saved: "+ fsize);
	    			} else {
	    				IJ.error("File is not saved correcly");
	    				f.delete();
	    				throw new FileNotFoundException();
	    				
	    			}
	    			
	    		} else {
	    			Log("Not a valid jar file or can not be downloaded");
	    		}
	    		return fsize;
	    	}

	    	/**
	    	 * @return
	    	 */
	    	private String extractJarName(URL url) {
	    		
	    		String retStr=extractJarName(url.getFile()) ;
	    		 
	    		return retStr;
	    	}
	    	
	    	
	    	private String extractJarName(String url) {
	    		
	    		String retStr="";
	    		try {
					StringTokenizer st=new StringTokenizer(url,"/");
					String s="";
					 while (st.hasMoreElements()) {
					       s = (st.nextElement()).toString();
					       if (s.compareToIgnoreCase(".jar")>0) retStr=s;
					}
					Log(retStr);
				} catch (NullPointerException e) {
					// TODO Auto-generated catch block
					Log("empty string");
					//e.printStackTrace();
				}
	    		return retStr;
	    	}

	    	private URL parseHTMLString(String html) 
	    		throws MalformedURLException {
	    		URL ret=null;
	    		// <a href="http://rsbweb.nih.gov/ij/plugins/download/jars/Image_Moments.jar">Image_Moments.jar</a>
	    		String afrag_start="<a href=";
	    		int offset_start=afrag_start.length();
	    		String afrag_end="</a>";
	    		 int offset_end=afrag_end.length();
	    		int index1=-1, index2=-1;
	    		
	    		index1=html.indexOf(afrag_start);
	    		index2=html.indexOf(afrag_end);
	    		
	    		if ((index1>-1) && (index1>-1) ) {
	    			String href=html.substring(index1+offset_start, index2 -offset_end);
	    			Log("link found: " + href);
	    			index1=href.indexOf('"');
	    			href=href.substring(index1+1, href.length());
	    			index2=href.indexOf('"');
	    			href=href.substring(index1, index2);
	    			//index2=href.indexOf('"');
	    			Log("url found: " + href);
	    			
	    		 
	    			
			    	ret = formURL(href);
	    			
	    		} 
	    		
	    		return ret;
	    	}

	    	
	    	//String urlreg="(http|https)://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
			/**
			 * @param href
			 * @return
			 * @throws MalformedURLException
			 */
			private URL formURL(String href) throws MalformedURLException {
				URL ret=null;
		
				String astr=extractJarName(href);
				if (!astr.isEmpty()) {
					ret = new URL(href);
				}
					/*
				if (proxyset) {
				 if (href.compareToIgnoreCase("//")>-1) {
						String prots=href.substring(0, href.indexOf(":"));
						int pl=prots.length();
						String rest=href.substring(pl+3, href.length());
						IJ.log(prots+ "  " +rest);
						try {
							ret = new URL(prots, proxyhost, proxyport, "//"+rest);
						} catch (RuntimeException e) {
							// Workaround a Java bug proxy
							ret = new URL(prots, proxyhost, proxyport, rest);
						}
				 }
					
					

				}*/
				 
				return ret;
			}
	    	
	    	/**
			 * @param dtde
			 * @param tr
			 * @param flavors
			 * @param i
			 * @throws UnsupportedFlavorException
			 * @throws IOException
			 */
			private boolean handleFile(Transferable tr,
					DataFlavor flavor) throws UnsupportedFlavorException,
					IOException {
				// Great!  Accept copy drops...
				  // Possible flavor: application/x-java-file-list; class=java.util.List
				//dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Log("Accepted file list drop.");
				boolean success=false;
				
				// And add the list of file names to our text area
				java.util.List list = (java.util.List)tr.getTransferData(flavor);
				for (int j = 0; j < list.size(); j++) {
				  Object o=list.get(j);
				  Log(o.toString() + "\n");
				  
				  File listfile=new File(o.toString() );
					 
				  success= copyJarFile( listfile,  pluginsDir) ;
				  Log("copied : " + success);
					if (success) filehandler.updateMenus();
				  
				}

				// If we made it this far, everything worked.
				//dtde.dropComplete(true);
				return success;
			}


			/**
			 * @param dtde
			 * @param tr
			 * @param flavors
	 
			 * @throws UnsupportedFlavorException
			 * @throws IOException
			 * @throws ConnectException
			 */
			private boolean handleObject( Transferable tr,
					DataFlavor  flavor) throws UnsupportedFlavorException,
					IOException, ConnectException {
				//dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Log("Accepted object drop.\n\n");
				Object o = tr.getTransferData(flavor);
				Log("Object: " + o.toString() +"\n");
				boolean success=false;
				/*
				if (o instanceof URL) {
					IJ.log("recognized as url ...  \n");
				}*/
				
				try {
					
		 
			    	jarURL=formURL(o.toString());
			    	
					Log("recognized as url: " + jarURL.toString() +"\n");
					success= getJarFile(jarURL, pluginsDir);
					Log("downloaded : " + success);
					if (success) filehandler.updateMenus();
					
				} catch (MalformedURLException ex) {
					IJ.error("malformed url");
				}
				
				//dtde.dropComplete(true);
				return success;
			}


			/**
			 * @param dtde
			 * @param tr
			 * @param flavors
			 * @param i
			 * @throws UnsupportedFlavorException
			 * @throws IOException
			 * @throws MalformedURLException
			 * @throws ConnectException
			 */
			private boolean handleHTML( Transferable tr,
					DataFlavor flavor) throws UnsupportedFlavorException,
					IOException, MalformedURLException, ConnectException {
				// Great!  Accept copy drops...
				//dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Log("Accepted html text drop.\n\n");
				boolean success=false;	
				// And add the list of file names to our text area
				InputStreamReader reader=(InputStreamReader)tr.getTransferData(flavor);
				 
				Log("encoding :" +reader.getEncoding());
				String html=readInput(reader);
				Log(html);
				jarURL= parseHTMLString(html);
				success= getJarFile(jarURL, pluginsDir);
				Log("downloaded : " + success);
				if (success) filehandler.updateMenus();
				
	  // If we made it this far, everything worked.
				//dtde.dropComplete(true);
				return success;
			}


			/**
			 * @param dtde
			 * @param tr
			 * @param flavor
			 * 
			 * @throws UnsupportedFlavorException
			 * @throws IOException
			 * @throws ConnectException
			 */
			private boolean handleURL( Transferable tr,
					DataFlavor  flavor) throws UnsupportedFlavorException,
					IOException, ConnectException {
				//if (flavor.isMimeTypeEqual("application/x-java-url") ) {
					    // Great!  Accept copy drops...
					    //dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					    Log("Accepted url list drop.\n\n");
						boolean success=false;			     
					    try {
					    	String aURLaddress =tr.getTransferData(flavor).toString();
					    	//Log(aURLaddress) ;
					    	jarURL=filehandler.formURL(aURLaddress);
					    	Log("recognized as url: " + jarURL.toString() +"\n");
					    	
					    	success=getJarFile(jarURL, pluginsDir);
					    	
					    	Log("downloaded : " + success);
							if (success) filehandler.updateMenus();
							
					    } catch (MalformedURLException ex) {
					    	IJ.error("malformed url");
					    }
					       
					   
					  // If we made it this far, everything worked.
					    //dtde.dropComplete(true);
					    return success;
					  
				  //}
			}


			/**
			 * @param reader
			 * @throws IOException
			 */
			private String readInput(InputStreamReader reader) throws IOException {
				BufferedReader br = new BufferedReader (reader);
	   
				StringBuffer sb=new StringBuffer(200);
				int utf16;
				

				  while ((utf16 = br.read()) > -1) {
					  byte utf8=(byte)((utf16 & 0x00FF) );
				      // sb.append (String.valueOf(utf16));
					  sb.append((char)utf8);
					    
				   }
				
				  br.close();
				//Log(sb.toString());  
				  System.out.print(sb.toString());
				  return sb.toString();
				
			}
			
			/* based on the ImageJ_Updater method */
		    
		    void updateMenus() {
		    	if (!IJ.versionLessThan("1.41l")) {
			 	long start = System.currentTimeMillis();
				Menus.updateImageJMenus();
				Log("Menus updated: "+(System.currentTimeMillis()-start)+" ms");
			
		    	} else  {
		    		showHelp(H_OLDVERSION);
		    	}
		    }
	    	
	    } /* end class */
	    
} /* end class */
