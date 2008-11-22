// This macro batch measures a folder of images.

    dir = getArgument; //can run from command line
    if (dir=="") dir = getDirectory("Choose a Directory ");
    start = getTime;
    list = getFileList(dir);
    run("Set Measurements...", "area mean standard min display");
    setBatchMode(true); // runs up to 6 times faster
    for (i=0; i<list.length; i++) {
        showProgress(i, list.length);
        open(dir+list[i]);
        run("Measure");
        close();
     }
     print((getTime-start)/1000, "seconds");


