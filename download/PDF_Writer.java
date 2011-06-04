// saves the active image as PDF using iText library
// author: J Mutterer and U Dittmer

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.*;
import ij.plugin.*;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class PDF_Writer implements PlugIn {

	static String PREF_KEY = "PDF_Writer.";

	static boolean showName=true,			// show the name of the image
					showSize=true,			// show the size in pixels of the image
					scaleToFit=true,		// scale proportionately to max. page width/heigth
					saveAllImages=false,	// save all images or just the frontmost one
					singleImage=false,		// save one image per page or as many as possible
					isLetter=true;			// output format is US Letter or A4

	public PDF_Writer() {
		showName = Prefs.get(PREF_KEY+"showName", true);
		showSize = Prefs.get(PREF_KEY+"showSize", true);
		scaleToFit = Prefs.get(PREF_KEY+"scaleToFit", true);
		saveAllImages = Prefs.get(PREF_KEY+"saveAllImages", false);
		singleImage = Prefs.get(PREF_KEY+"singleImage", false);
		isLetter = Prefs.get(PREF_KEY+"isLetter", true);
	}

	public void run (String arg) {
		if (WindowManager.getCurrentImage() == null) {
			IJ.showStatus("No image is open");
			return;
		}

		GenericDialog gd = new GenericDialog("PDF Writer");
		gd.addCheckbox("Show image name", showName);
		gd.addCheckbox("Show image size", showSize);
		gd.addCheckbox("Scale to fit", scaleToFit);
		gd.addCheckbox("Save all images", saveAllImages);
		gd.addCheckbox("One image per page", singleImage);
		gd.addCheckbox("US Letter", isLetter);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		showName = gd.getNextBoolean();
		showSize = gd.getNextBoolean();
		scaleToFit = gd.getNextBoolean();
		saveAllImages = gd.getNextBoolean();
		singleImage = gd.getNextBoolean();
		isLetter = gd.getNextBoolean();

		Prefs.set(PREF_KEY+"showName", showName);
		Prefs.set(PREF_KEY+"showSize", showSize);
		Prefs.set(PREF_KEY+"scaleToFit", scaleToFit);
		Prefs.set(PREF_KEY+"saveAllImages", saveAllImages);
		Prefs.set(PREF_KEY+"singleImage", singleImage);
		Prefs.set(PREF_KEY+"isLetter", isLetter);

        String name = IJ.getImage().getTitle();
        SaveDialog sd = new SaveDialog("Save as PDF", name, ".pdf");
        name = sd.getFileName();
        String directory = sd.getDirectory();
        String path = directory+name;
        Document document = new Document(isLetter ? PageSize.LETTER : PageSize.A4);
		document.addCreationDate();
		document.addTitle(name);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();
			Paragraph p;
			String printName;
			java.awt.Image awtImage;
			Image image;
			boolean isFirst = true;

			for (int idx=1; idx<=WindowManager.getWindowCount(); idx++) {
				if (! isFirst) {
					if (singleImage) {
						document.newPage();
					} else {
						document.add(new Paragraph("\n"));
						float vertPos = writer.getVerticalPosition(true);
						PdfContentByte cb = writer.getDirectContent();
						cb.setLineWidth(1f);
						if (isLetter) {
							cb.moveTo(PageSize.LETTER.getLeft(50), vertPos);
							cb.lineTo(PageSize.LETTER.getRight(50), vertPos);
						} else {
							cb.moveTo(PageSize.A4.getLeft(50), vertPos);
							cb.lineTo(PageSize.A4.getRight(50), vertPos);
						}
						cb.stroke();
					}
				}

				if (saveAllImages) {
					awtImage = WindowManager.getImage(idx).getImage();
					printName = WindowManager.getImage(idx).getTitle();
				} else {
					awtImage = WindowManager.getCurrentImage().getImage();
					printName = name;
				}

				if (showName) {
					p = new Paragraph(printName);
					p.setAlignment(p.ALIGN_CENTER);
					document.add(p);
				}
				if (showSize) {
					p = new Paragraph(awtImage.getWidth(null)+" x "+ awtImage.getHeight(null));
					p.setAlignment(p.ALIGN_CENTER);
					document.add(p);
				}

				image = Image.getInstance(awtImage, null);
				if (scaleToFit) {
					if (isLetter)
						image.scaleToFit(PageSize.LETTER.getRight(50), PageSize.LETTER.getTop(50));
					else
						image.scaleToFit(PageSize.A4.getRight(50), PageSize.A4.getTop(50));
				}
				image.setAlignment(image.ALIGN_CENTER);
				document.add(image);

				isFirst = false;
				if (! saveAllImages)
					break;
			}
		} catch(DocumentException de) {
			IJ.showMessage("PDF Writer", de.getMessage());
		} catch(IOException ioe) {
			IJ.showMessage("PDF Writer", ioe.getMessage());
		}
		document.close();
		IJ.showStatus("");
	}
}

