// Writes a sine/cosine table directly to a file 
// then opens that file in the "Log" window.

   // Save table, prompting for file name
   f = File.open("");
   print(f, "i\tsin(i)\tcos(i)");
   for (i=0; i<=2*PI; i+=0.01)
      print(f, i + "  \t" + sin(i) + " \t" + cos(i));
   File.close(f);

   // Open table, prompting for file name
   s = File.openAsString("");
   print(s);

   // Use the Import_Results_Table macro to open in results table
   // Assumes file is at "/Users/wayne/Desktop/log.txt"
   dir = getDirectory("plugins")+" Conference"+File.separator;
   runMacro(dir+"Import_Results_Table.ijm", "/Users/wayne/Desktop/log.txt");
