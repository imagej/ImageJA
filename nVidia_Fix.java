import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import ij.IJ;
import ij.plugin.PlugIn;

public class nVidia_Fix implements PlugIn {
	static final String ps="-Dsun.java2d.noddraw=true ";

	@Override
	public void run(String arg) {
	 
		if (!IJ.isWindows()) {
			IJ.error("nVidia Fix", "This plugin is Windows-only.");
			return;
		}
		String path = IJ.getDirectory("imagej")+"ImageJ.cfg";
		File f=new File(path);
		if (!(f.isFile()&&f.exists())) {
			IJ.error("nVidia Fix", "ImageJ configuration file not found at\n \n"+path);
			return;
		}
		fix(f);
		IJ.showMessage("nVidia Fix", "The \""+ps+"\" option has\n"
			+ "been added to the ImageJ configuration file at\n \n   "
			+ path
			+ "\n \nPlease restart ImageJ.\n \n"
			+ "Delete \"ImageJ.cfg\" if there is a problem.");
	}

	private void fix(File f) {
		try {
			RandomAccessFile input= new RandomAccessFile(f, "rw");
			FileChannel channel = input.getChannel();
			int fileLength = (int)channel.size();
			//System.out.println ("-file found: "+f.getAbsolutePath() +" " + fileLength);
			MappedByteBuffer buffer = 
				channel.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);

			// ISO-8859-1  is ISO Latin Alphabet #1
			Charset charset = Charset.forName("ISO-8859-1");
			CharsetDecoder decoder = charset.newDecoder();

			CharBuffer charBuffer = decoder.decode(buffer);

			String fs=charBuffer.toString();

			//System.out.println ("-fs >>: "+fs);
			if (fs.indexOf(ps)>0) return;

			int ind=fs.indexOf("-cp");
			//System.out.println ("-cp: "+ind);

			String out=fs.substring(0, ind);
			//ps+=fs.substring(ind);
			out+=ps+fs.substring(ind);
			charBuffer.position(ind);
			//System.out.println ("-ps: "+out);

			charBuffer=CharBuffer.allocate(out.length());
			charBuffer.put(out);

			charBuffer.clear();
			fs=charBuffer.toString();
			charBuffer.clear();
			//System.out.println ("-fs2: "+fs);

			CharsetEncoder encoder = charset.newEncoder();
			channel.write(encoder.encode(charBuffer), 0);
			channel.close();
			input.close();
		} catch (IOException e) {
			IJ.handleException(e);
		}

	}

}
