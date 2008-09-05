This folder contains example Javascript scripts that
ImageJ 1.41 or later can run.

Use the ImageJ text editor's Macros>Evaluate Javascript
or its Macros>Run Macro command (requires ".js" extension)
to run JavaScript code.

The required JavaScript interpreter
(Mozilla Rhino) is built into Java 1.6 for Linux and Windows.
Mac users, and users of earlier versions of Java, must
download JavaScript.jar into the plugins folder.

  http://rsb.info.nih.gov/ij/download/tools/JavaScript.jar

To run a script download it, drag and drop it on the 
"ImageJ" window, then press ctrl-r (Macros>Run Macro).
Or copy it the clipboard, press shift+v (File>New>System Clipboard),
then press ctrl+j (Macros>Evaluate JavaScript).

Albert Cardona's Javascript Scripting tutorial has more examples:

  http://pacific.mpi-cbg.de/wiki/index.php/Javascript_Scripting

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


