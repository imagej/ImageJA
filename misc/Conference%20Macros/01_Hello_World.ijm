
// To display in "Log" window

  print("Hello World!");


// To display in a dialog:

  showMessage("Hello World!");


// To display in a dialog with a title:

  showMessage("Hello", "Hello World!");


/* The Java version looks like this:

    public class HelloWorld {
       static public void main( String args[] ) {
         System.out.println( "Hello World!" );
       }
    }

The ImageJ plugin version looks like this:

   import ij.*;
   import ij.plugin.*;
   public class Hello_World implements PlugIn {
      public void run(String arg) {
         IJ.log("Hello World!");
      }
   }

"The Hello World Collection" at
http://www.roesler-ac.de/wolfram/hello.htm
includes 263 Hello World programs.


