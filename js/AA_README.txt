This folder contains example Javascript scripts that
ImageJ 1.41 and later can run. To run JavaScript, ImageJ
uses the Mozilla Rhino interpreter built into Java 1.6
for Linux and Windows. Mac users, and users of earlier
versions of Java, must download JavaScript.jar into the
plugins folder. This JAR file is available at:

  http://rsb.info.nih.gov/ij/download/tools/JavaScript.jar

To run a script, download it, drag and drop it on the 
"ImageJ" window, then press ctrl-r (Macros>Run Macro).
Or copy it the clipboard, press shift+v (File>New>System Clipboard),
then press ctrl+j (Macros>Evaluate JavaScript).

The 'print' function is predefined as

   function print(s) {IJ.log(s);}
   
so print("Hello world") works as expected.

By default, the following ImageJ and Java classes are imported:

   ij.*
   ij.gui.*
   ij.process.*
   ij.measure.*
   java.lang.*
   java.awt.*

Additional classes can be imported using importClass()
and importPackage(), for example:

   importPackage(java.io); // import all java.io classes
   importClass(java.io.File) // import java.io.File
   importPackage(Packages.ij.plugin.frame); // import frame package
   importClass(Packages.ij.plugin.frame.Editor); // import Editor class

Albert Cardona's Javascript Scripting tutorial has more examples:

  http://pacific.mpi-cbg.de/wiki/index.php/Javascript_Scripting
  

Advantages and Disadvantages of JavaScript

   Advantages:
      - Full access to ImageJ and Java APIs
      - Richer language (objects, "?" operator, break, continue, etc.)
   
   Disadvatages:
      - Slower, especally starting up
      - No equivalent of macro sets
      - Cannot access most built in macro functions
      - No support for "batch mode"
      - Cannot create tools and toolbar menus
      - Requires JavaScript.jar on Macs
      

ImageJ commands and macro functions that support JavaScript:

  1. Plugins>New>javaScript: opens a new text window with the title
     "Script.js". As a shortcut, type shift+n (Edit>New>Text Window)
     and ctrl+j (Macros>Evaluate>javaScript), which changes the title
     to "Unitled.js".
    
  2. Macros>Evaluate JavaScript (in the editor): runs JavaScript
     code in the editor window. As a shortcut, type ctrl+j. 
     
  3. Macros>Run Macro (in the editor): runs JavaScript code
     if the title ends with ".js". As a shortcut, type ctrl+r. 

  4. Plugins>Macros>Run: runs a Javascript 
     program contained in a ".js" file.
     
  5. Help>Update Menus (runs when ImageJ starts): installs
     JavaScript (".js") programs located in the plugins folder
     or subfolders into the Plugins menu or submenus.

  6. eval("script", string) macro function: runs the javaScript
     contained in 'string' in the current thread.

  7. run("Run...", "run=<path>/name.js") macro function: runs
     "name.js" in a separate thread.

  8. eval('script', File.openAsString("<path>/name.js")) macro 
     function: runs "name.js" in the current thread.
     
