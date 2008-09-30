// Recursively lists the files in a user-specified directory.
// Open a file on the list by double clicking on it.

  dir = getDirectory("Choose a Directory ");
  count = 1;
  listFiles(dir); 

  function listFiles(dir) {
     list = getFileList(dir);
     for (i=0; i<list.length; i++) {
        if (endsWith(list[i], "/"))
           listFiles(""+dir+list[i]);
        else
           print((count++) + ": " + dir + list[i]);
     }
  }
