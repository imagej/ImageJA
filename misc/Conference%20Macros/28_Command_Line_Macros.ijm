Run macros from the command line

ImageJ command line options:

  -macro path [arg]
  -batch path [arg]
  -eval "macro code"

Examples:

   cd /users/wayne/ImageJ/Conference/images
   imagej FluorescentCells.jpg
   imagej -eval "makeOval(90,90,300,300); fill;"
   imagej -eval "return getDirectory('current')"
   imagej -macro Polygon
   imagej -batch BatchMeasure.ijm /Users/wayne/ImageJ/Conference/images/

To create 'imagej' command:
  Unix: alias imagej 'java -Xmx400m -jar /Users/wayne/ImageJ/ij.jar -ijpath /Users/wayne/ImageJ'
  Windows: Add ImageJ folder to PATH environment variable
