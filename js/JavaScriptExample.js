// Run this script in ImageJ 1.41e or later using
// the Macro Editor's Macro>Evaluate JavaScript 
// command.

// open an image
IJ.run("Blobs (25K)");
img = IJ.getImage();
ip = img.getProcessor();

// invert it quickly
start = System. currentTimeMillis();
ip.invert();
img.updateAndDraw();
print("fast invert: "+(System. currentTimeMillis()-start)/1e3+" sec");

// invert it slowly
// runs faster if 'var' is used to declare variables
var w = ip.getWidth();
var h = ip.getHeight();
var start = System.currentTimeMillis();
for (var y=0; y<h; y++) {
   IJ.showProgress(y, h-1);
   for (var x=0; x<w; x++) {
      ip.putPixel(x, y, 255-ip.getPixel(x, y));
   }
}
img.updateAndDraw();
print("slow invert: "+(System. currentTimeMillis()-start)/1e3+" sec");

// list properties of 'img' object
  img = IJ.getImage();
  for (name in img) {
    type = typeof(img[name]);
    hdr = type!="function"?type+" "+name:"";
    print("\n"+hdr);
    print(img[name]);
  }
 
// show date and time
IJ.showMessage("Date and Time", new Date());


