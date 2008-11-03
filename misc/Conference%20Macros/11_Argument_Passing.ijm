// This macro demonstrates how to use string concatenation to 
// pass  variables  to commands called using the run() function.
// It creates an image, sets the scale to 150 pixels/mm, rotates
// 25 degrees, and saves in Analyze format. Note that the file
// path must be enclosed in brackets because of the space in
// the image name.

name = "Test Image";
directory = "/Users/wayne/Desktop/";
width = 400;
height = 300;
scale = 150; // 150 pixels/mm
unit = "mm";
angle = 25;
newImage(name, "8-bit ramp", width, height, 1);
run("Set Scale...", "distance="+scale+" known=1 pixel=1 unit="+unit);
run("Arbitrarily...", "interpolate  angle="+angle);
path = "["+ directory +name+"]";
run("Analyze Writer", "save="+path);
