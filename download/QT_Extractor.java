import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.*;
import ij.plugin.*;
import quicktime.*;
import quicktime.std.qtcomponents.*;
import quicktime.io.*;
import quicktime.app.view.QTImageProducer;
import quicktime.std.movies.Movie;
import quicktime.qd.Pict;
import quicktime.app.view.MoviePlayer;
import quicktime.std.movies.MovieController;
import java.util.*;
import ij.io.*;
import quicktime.qd.QDDimension;
import java.awt.event.*;

public class QT_Extractor implements PlugIn, ActionListener {

	String frames, temp, ext;
	boolean disp;
	double fps;
	GenericDialog gd;
	Button browse1, browse2;
	int nframesbefore=3;
	int nframesafter=2;

	static String path = "";
	static String dir = "";
	static String framesAndLabels = "";

	public void run(String arg)
	{
		int pos, frame = 12, cnt, min = 0, sec = 0, fr = 0, i, j, k, red, green, blue, w = 720, h = 480;
		String newpathe, newpatho, label;

		getData();
		if (gd.wasCanceled()) {
			return;
		}

		try
		{
			QTSession.open();
			QTFile file = new QTFile(path);
			OpenMovieFile omf = OpenMovieFile.asRead(file);
			Movie m = Movie.fromFile(omf);
			MoviePlayer mp = new MoviePlayer(m);
			MovieController movieController = new MovieController(m);


			int old;
			int count = 0;

			frames = frames.trim();
			frames = frames.concat("\n");
			int length = frames.length();
			for (i = 0; i < length; i++) {
				if (frames.charAt(i) == '\n') {
					count++;
				}
			}

			label = "";
			old = 0;

			for (i = 0; i < count; i++)
			{
				for (j = old; j < length; j++)
				{
					if (frames.charAt(j)=='\t' || frames.charAt(j)==' ')
					{
						break;
					}
				}
				label = frames.substring(old, j);
				label = label.trim();
			// minutes
				j++;
				old = j;
				temp = "";
				for (j = j; j < length; j++)
				{
					if ((frames.charAt(j)) == '.') {
						break;
					}
				}
				temp = frames.substring(old, j);
				if (temp.length() == 1) {
					temp = "0" + temp;
				}
				min = 10 * (temp.charAt(0) - 48);
				min = min + (temp.charAt(1) - 48);
			// seconds
				j++;
				old = j;
				temp = "";
				for (j = j; frames.charAt(j) != '.'; j++) {
					if ((frames.charAt(j)) == '.') {
						break;
					}
				}
				temp = frames.substring(old, j);
				if (temp.length() == 1) {
					temp = "0" + temp;
				}
				sec = 10 * (temp.charAt(0) - 48);
				sec = sec + (temp.charAt(1) - 48);
			// frames
				j++;
				old = j;
				temp = "";
				for (j = j; frames.charAt(j) != '\n'; j++) {
					if ((frames.charAt(j)) == '.') {
						break;
					}
				}
				temp = frames.substring(old, j);
				if (temp.length() == 1) {
					temp = "0" + temp;
				}
				if (temp.length() == 2) {
					temp = "0" + temp;
				}
				old = j;

				fr = 100 * (temp.charAt(0) - 48);
				fr = fr + 10 * (temp.charAt(1) - 48);
				fr = fr + (temp.charAt(2) - 48);
			// end text parsing


			// position calculation
				if(fps==29.97)
				{
					frame = (int)(min*1800 + sec*30 + fr) - (2*min) + 1;
//					frame = frame + (int)Math.round((frame-1)/999);
				}
				else
				{
					frame = (int)(min * 60 * fps) + (int)(sec * fps) + fr +1;
				}

				int p;
				int[] pixels;

				for(p=(-1*nframesbefore);p<(1+nframesafter);p++)
				{
					pos = (int) ((frame+p) * (mp.getScale() / fps));

					mp.setTime(pos);

					label = label.trim();

//	IJ.showMessage(IJ.d2s(p));

					cnt = 1;

					while (true)
					{
						if(p<0)
						{
							if(Math.abs(p)<10)
							{
								newpathe = dir + label + IJ.d2s(cnt,0) + "_" + "-0" + IJ.d2s(Math.abs(p), 0) + "e." + ext;
								newpatho = dir + label + IJ.d2s(cnt,0) + "_" + "-0" + IJ.d2s(Math.abs(p), 0) + "o." + ext;
							}
							else
							{
								newpathe = dir + label + IJ.d2s(cnt,0) + "_" + "-" + IJ.d2s(Math.abs(p), 0) + "e." + ext;
								newpatho = dir + label + IJ.d2s(cnt,0) + "_" + "-" + IJ.d2s(Math.abs(p), 0) + "o." + ext;
							}
						}
						else
						{
							 if(p==0)
							{
//IJ.showMessage("here");
									newpathe = dir + label + IJ.d2s(cnt,0) + "_" + "0" + "e." + ext;
									newpatho = dir + label + IJ.d2s(cnt,0) + "_" + "0" + "o." + ext;
							}
							else
							{
								if(p<10)
								{
									newpathe = dir + label + IJ.d2s(cnt,0) + "_" + "0" + IJ.d2s(Math.abs(p), 0) + "e." + ext;
									newpatho = dir + label + IJ.d2s(cnt,0) + "_" + "0" + IJ.d2s(Math.abs(p), 0) + "o." + ext;
								}
								else
								{
									newpathe = dir + label + IJ.d2s(cnt,0) + "_" + IJ.d2s(Math.abs(p), 0) + "e." + ext;
									newpatho = dir + label + IJ.d2s(cnt,0) + "_" + IJ.d2s(Math.abs(p), 0) + "o." + ext;
								}
							}
						}

						if (!( (new File(newpathe).exists())  || (new File(newpatho).exists()) )) {
							break;
						}

						cnt = cnt + 1;
					}

					QDDimension dim = mp.getOriginalSize();
					QTImageProducer qtip = new QTImageProducer(mp, new Dimension(dim.getWidth(), dim.getHeight()));
					Image img = Toolkit.getDefaultToolkit().createImage(qtip);
					ImagePlus even = new ImagePlus(label,img);
					ImagePlus odd = new ImagePlus(label,img);
					ImageProcessor even_ip = even.getProcessor();
					ImageProcessor odd_ip = odd.getProcessor();

				//	IJ.showMessage("Here");

				// Even deinterlacing
					pixels = (int[])even_ip.getPixels();
					int offset, position;
					int avg;

					for(int r = 2; r< (h-1); r+=2)
					{
						offset = r * w;
						for(int c=2;c<(w-1);c++)
						{
							position = offset + c;

								red = (int)((pixels[position-w] & 0xff0000)>>16);
								green = (int)((pixels[position-w] & 0x00ff00)>>8);
								blue = (int)(pixels[position-w] & 0x0000ff);

								red = red + (int)((pixels[position+w] & 0xff0000)>>16);
								green = green + (int)((pixels[position+w] & 0x00ff00)>>8);
								blue = blue + (int)(pixels[position+w] & 0x0000ff);

								red = red/2;
								green = green/2;
								blue = blue/2;

								pixels[position] = (int)(((red & 0xff)<<16)+((green & 0xff)<<8) + (blue & 0xff));
						}
					}


				// Odd deinterlacing
					pixels = (int[])odd_ip.getPixels();

					for(int r = 1; r< (h-1); r+=2)
					{
						offset = r * w;
						for(int c=2;c<(w-1);c++)
						{
							position = offset + c;
							red = (int)((pixels[position-w] & 0xff0000)>>16);
							green = (int)((pixels[position-w] & 0x00ff00)>>8);
							blue = (int)(pixels[position-w] & 0x0000ff);

							red = red + (int)((pixels[position+w] & 0xff0000)>>16);
							green = green + (int)((pixels[position+w] & 0x00ff00)>>8);
							blue = blue + (int)(pixels[position+w] & 0x0000ff);

							red = red/2;
							green = green/2;
							blue = blue/2;

							pixels[position] = (int)(((red & 0xff)<<16)+((green & 0xff)<<8) + (blue & 0xff));
						}
					}
				// End of Deinterlacing


					if (disp)
					{
						even.show();
						even.updateAndDraw();
						odd.show();
						odd.updateAndDraw();
					}

					if (ext == "jpg")
					{
						new FileSaver(even).saveAsJpeg(newpathe);
						new FileSaver(odd).saveAsJpeg(newpatho);
					}
					else
					{
						new FileSaver(even).saveAsTiff(newpathe);
						new FileSaver(odd).saveAsTiff(newpatho);
					}
				}



			}
		}
		catch (QTException e) {
			IJ.showMessage("QT Extractor", "There\'s been an error.\n \n"+e);
		}

		QTSession.close();
		IJ.showStatus("QT Extractor Finished");
	}


	public void getData() {
		String[] choices = {"jpg", "tif"};
		gd = new GenericDialog("QT_Extractor");
		gd.addStringField("Path to QT File:", path, 40);
		gd.addPanel(makePanel(gd, 1));
		gd.addMessage("");
		gd.addStringField("Save Directory:", dir, 40);
		gd.addPanel(makePanel(gd, 2));
		gd.addMessage("");
		gd.addNumericField("FPS:", 29.97, 2);
		gd.addChoice("Save as Type: ", choices, "jpg");


		gd.addMessage("Extract");
		gd.addNumericField("This many frames before:", 0, 0);
		gd.addNumericField("This many frames after:", 0, 0);
		gd.addCheckbox("Open pictures in ImageJ",false);
		gd.addMessage("Enter the frames and file labels below.\n(Format: my_label -tab- min.sec.frame)");
		gd.addTextAreas(framesAndLabels, null, 6, 30);
		gd.showDialog();

		path = gd.getNextString();
		dir = gd.getNextString();
		disp = gd.getNextBoolean();
		ext = gd.getNextChoice();
		fps = gd.getNextNumber();
		nframesbefore = (int)gd.getNextNumber();
		nframesafter = (int)gd.getNextNumber();
		frames = gd.getNextText();
	}

	Panel makePanel(GenericDialog gd, int id) {
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		if (id==1) {
			browse1 = new Button("Browse...");
			browse1.addActionListener(this);
			panel.add(browse1);
		} else {
			browse2 = new Button("Browse...");
			browse2.addActionListener(this);
			panel.add(browse2);
		}
		return panel;
	}

	void getPath() {
		OpenDialog od = new OpenDialog("Select a QuickTime File...", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;
		Vector stringField = gd.getStringFields();
		TextField tf = (TextField)(stringField.elementAt(0));
		tf.setText(directory+name);
	}

	void getDir() {
		SaveDialog sd = new SaveDialog("Open destination folder...", "dummy name (required)", "");
		if (sd.getFileName()==null)
			return;
		Vector stringField = gd.getStringFields();
		TextField tf = (TextField)(stringField.elementAt(1));
		tf.setText(sd.getDirectory());
	}


	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==browse1)
			getPath();
		else if (source==browse2)
			getDir();
	}

}




