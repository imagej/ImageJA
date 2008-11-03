// This macro demonstrates how a macro can display a
// data input dialog box. This dialog contains one string 
// field, one drop down menu, two numeric fields,   
// and one check box.

  title = "Test Image";
  width=512; height=512;
  Dialog.create("New Image");
  Dialog.addString("Title:", title);
  Dialog.addChoice("Type:", newArray("8-bit", "16-bit", "32-bit", "RGB"));
  Dialog.addNumber("Image_Width:", 512);
  Dialog.addNumber("Image_Height:", 512);
  Dialog.addCheckbox("Ramp", true);
  Dialog.show();
  title = Dialog.getString();
  width = Dialog.getNumber();
  height = Dialog.getNumber();
  type = Dialog.getChoice();
  ramp = Dialog.getCheckbox();
  if (ramp==true) type = type + " ramp";
  newImage(title, type, width, height, 1);
  exit;

This is what gets recorded when you run this macro from the plugins with the recorder running:

  run("Dialog Demo", "title=[Test Image] image_width=512 image_height=512 type=8-bit ramp");

The run() method has this format:

  run(command, options)

where options is a string consisting of key=value pairs. The key is the first word
of the component label and the value is the component value. Add underscores to
labels to make the keys unique. Values with spaces must be enclosed in brackets.
Checkboxes only generate a key (if true) or nothing (if false).




