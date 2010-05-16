package ij.io;
import ij.IJ;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Vector;



/** ImageJ uses this class loader to load plugins and resources from the
 * plugins directory and immediate subdirectories. This class loader will
 * also load classes and resources from JAR files.
 *
 * <p> The class loader searches for classes and resources in the following order:
 * <ol>
 *  <li> Plugins directory</li>
 *  <li> Subdirectories of the Plugins directory</li>
 *  <li> JAR and ZIP files in the plugins directory and subdirectories</li>
 * </ol>
 * <p> The class loader does not recurse into subdirectories beyond the first level.
*/
public class PluginClassLoader extends URLClassLoader {
 //   protected String path;

    /**
     * Creates a new PluginClassLoader that searches in the directory path
     * passed as a parameter. The constructor automatically finds all JAR and ZIP
     * files in the path and first level of subdirectories. The JAR and ZIP files
     * are stored in a Vector for future searches.
     * @param path the path to the plugins directory.
     */
 	public PluginClassLoader(String path) {
		super(new URL[0], IJ.class.getClassLoader());
		//init(path);
		try {
			addDir(  path, true);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** This version of the constructor is used when ImageJ is launched using Java WebStart. */
 	public PluginClassLoader(String path, boolean callSuper) {
		super(new URL[0], Thread.currentThread().getContextClassLoader());
		//init(path);
		/*try {
			addDir(  path, true);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

 	protected Vector<String> filesv=new Vector<String>();
	
	   

    public  void addFile (String path) throws MalformedURLException
    {
        String urlPath = "jar:file://" + path + "!/";
        System.out.println("adding path: "+urlPath);
        addURL (new URL (urlPath));
       // addURL(f.toURI().toURL());
        filesv.add(path);
    }
    
    public String[] getJars() {
    	String[] ret=new String[ filesv.size()];
    	int cnt=0;
    	Enumeration<String> e=filesv.elements();
    	while ( e.hasMoreElements()) {
    		ret[cnt]= e.nextElement();
    		cnt++;
    	}
    	return ret;
    	
    }
    
    protected  void addDir(String path) throws MalformedURLException
    {
    	File f= new File(path);
    	
    	if (f.exists() && f.isDirectory()) {
    		System.out.println("browsing " + f.getAbsolutePath());
    		filesv.add(f.getAbsolutePath());
    		
    		//find all JAR files on the path and subdirectories
    		 
         
            // Add plugin directory to search path
            addURL(f.toURI().toURL());
           
    		
    		File[] jars=f.listFiles(new JarFilter());
    		
    		for (int i=0; i<jars.length; i++) {
    			 String urlPath = "jar:file://" + jars[i].getAbsolutePath() + "!/";
    		        System.out.println("adding path: "+jars[i].getAbsolutePath());
    		        //addURL (new URL (urlPath));
    		        addURL(jars[i].toURI().toURL());
    		        filesv.add(jars[i].getAbsolutePath());
    		}
    		
    		File[]  zips=f.listFiles(new ZipFilter());
    		
    		for (int i=0; i<zips.length; i++) {
    			 //String urlPath = "jar:file://" + jars[i].getAbsolutePath() + "!/";
    		        System.out.println("adding path: "+zips[i].getAbsolutePath());
    		       // addURL (new URL (urlPath));
    		        addURL(zips[i].toURI().toURL());
    		        filesv.add(zips[i].getAbsolutePath());
    		}
    		
    		
    	}
       
    }
    
    public  void addDir(String path, boolean recursive) throws MalformedURLException {
    	addDir( path);
    	if (recursive) {
     
    		File f= new File(path);
	    	
	    	if (f.exists() && f.isDirectory()) {
	    		File[] flist= f.listFiles();
	    		int  cnt=0;
	    		for (int i=0; i<flist.length; i++) {
	    			if (flist[i].isDirectory()) cnt++;
	    		}
	    		File[] dirlist=new File[cnt];
	    		cnt=0;
	    		for (int i=0; i<flist.length; i++) {
	    			if (flist[i].isDirectory()) { 
	    				dirlist[cnt]=flist[i];
	    				cnt++;
	    			}
	    		}
	    		for (int i=0; i<dirlist.length; i++) {
	    			 
	    			addDir(dirlist[i].getAbsolutePath(),  recursive);
	    		}
	    	}
    	}
    }
    
    public void addJars (String s[], int offset) throws MalformedURLException {
    	if (offset<0) offset=0;
    	if (offset>s.length-1) offset=0;
    	for (int i=offset; i<s.length; i++) {
    		addFile (s[i]);
    	}
    	 
    }
    
    
 /*
	
	void init(String path) {
		//this.path = path;

		//find all JAR files on the path and subdirectories
		File f = new File(path);
        try {
            // Add plugin directory to search path
            addURL(f.toURI().toURL());
        } catch (MalformedURLException e) {
            ij.IJ.log("PluginClassLoader: "+e);
        }
		String[] list = f.list();
		if (list==null)
			return;
		for (int i=0; i<list.length; i++) {
			if (list[i].equals(".rsrc"))
				continue;
			f=new File(path, list[i]);
			if (f.isDirectory()) {
                try {
                    // Add first level subdirectories to search path
                    addURL(f.toURI().toURL());
                } catch (MalformedURLException e) {
            		ij.IJ.log("PluginClassLoader: "+e);
                }
				String[] innerlist = f.list();
				if (innerlist==null) continue;
				for (int j=0; j<innerlist.length; j++) {
					File g = new File(f,innerlist[j]);
					if (g.isFile()) addJAR(g);
				}
			} else 
				addJAR(f);
		}
	}

    private void addJAR(File f) {
        if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
            try {
                addURL(f.toURI().toURL());
            } catch (MalformedURLException e) {
				ij.IJ.log("PluginClassLoader: "+e);
            }
        }
    }
 */
    class JarFilter implements FilenameFilter {
    	  final String pattern=".jar";
    		@Override
    		public boolean accept(File f, String name) {
    			 return name.toLowerCase().endsWith(pattern.toLowerCase());
    	 
    		}
    		  
    }
    
    class ZipFilter implements FilenameFilter {
  	  final String pattern=".zip";
  		@Override
  		public boolean accept(File f, String name) {
  			 return name.toLowerCase().endsWith(pattern.toLowerCase());
  	 
  		}
  		  
  }
}
