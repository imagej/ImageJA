// Command Launcher
//
// This script displays a little dialog window that lets you type in a 
// an ImageJ command. As the name of the command is typed, the
// color of the text changes from red to black if the command exists.
// It is from the Fiji "Scripting Comparisons" page at
//   http://pacific.mpi-cbg.de/wiki/index.php/Scripting_comparisons

  importClass(Packages.java.util.ArrayList);
  importClass(Packages.java.awt.event.TextListener);

  commands = Menus.getCommands();
  keys = new ArrayList(commands.keySet());
  gd = new GenericDialog("Run Command");
  gd.addStringField("Command: ", "");
  prom = gd.getStringFields().get(0);

  body = { textValueChanged: function(evt) {
        text = prom.getText();
        for (i=0;i<keys.size();i++) {
           command = keys.get(i);
           if (command.equals(text)) {
              prom.setForeground(Color.black);
              return;
           }
        }
        prom.setForeground(Color.red); // not found
     }
  }
  prom.addTextListener(new TextListener(body));

  gd.showDialog();
  if (!gd.wasCanceled())
     IJ.doCommand(gd.getNextString());
