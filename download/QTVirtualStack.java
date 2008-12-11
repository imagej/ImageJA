import ij.VirtualStack;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import quicktime.app.view.MoviePlayer;
import quicktime.app.view.QTImageProducer;
import quicktime.io.OpenMovieFile;
import quicktime.io.QTFile;
import quicktime.qd.QDDimension;
import quicktime.std.StdQTConstants;
import quicktime.std.movies.Movie;
import quicktime.std.movies.TimeInfo;
import quicktime.std.movies.Track;
import quicktime.QTException;
import java.util.ArrayList;
import java.util.List;
import java.awt.Dimension;
import java.awt.image.PixelGrabber;


public class QTVirtualStack extends VirtualStack {
  final private MoviePlayer player;
  final QTImageProducer imageProducer;
  final private int height;
  final private int width;
  final private ArrayList frameLocations = new ArrayList();


  public QTVirtualStack(QTFile qtf) {

    try {
      OpenMovieFile qtMovieFile = OpenMovieFile.asRead(qtf);
      Movie movie = Movie.fromFile(qtMovieFile);
      Track visualTrack = movie.getIndTrackType (1,
						 StdQTConstants.visualMediaCharacteristic,
						 StdQTConstants.movieTrackCharacteristic);
      QDDimension d = visualTrack.getSize();
      int nFrames = visualTrack.getMedia().getSampleCount();

      this.width  = d.getWidth();
      this.height = d.getHeight();
      this.player = new MoviePlayer(movie);
      this.imageProducer = new QTImageProducer(this.player, new Dimension(this.width, this.height));
      

      {
	int location = player.getTime();
	for (int frame = 0 ; frame < nFrames ; frame++) {
	  this.frameLocations.add(new Integer(location));
	  TimeInfo ti = visualTrack.getNextInterestingTime(StdQTConstants.nextTimeMediaSample,
							   location,
							   1);
	  location = ti.time;
	}
      }
    }
    catch (QTException qte) {
      throw new RuntimeException(qte);
    }
  }


  public int getHeight() {
    return this.height;
  }


  public int getWidth() {
    return this.width;
  }


  public int getSize() {
    return frameLocations.size();
  }


  public String getSliceLabel(int slice) {
    slice--;  // ImageJ slices are 1-based rather than zero based.
    return "";
  }


  public ImageProcessor getProcessor(int slice) {
    slice--;  // ImageJ slices are 1-based rather than zero based.

    try {
      int w = this.getWidth();
      int h = this.getHeight();
      int[] pixels = new int[w * h];
      this.player.setTime(((Integer)frameLocations.get(slice)).intValue());
      this.imageProducer.redraw(null);
      PixelGrabber grabber = new PixelGrabber(this.imageProducer,
					      0, 0,
					      w, h,
					      pixels,
					      0, w);
      grabber.grabPixels();
      
      return new ColorProcessor(w, h, pixels);
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteSlice(int slice) {
    slice--;  // ImageJ slices are 1-based rather than zero based.
    frameLocations.remove(slice);
  }
}
