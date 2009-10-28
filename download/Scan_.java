/*
A scanner plugin for ImageJ which uses the free mm's computing java library 
(available at http://www.mms-computing.co.uk/)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerDevice;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;
import uk.co.mmscomputing.device.scanner.ScannerListener;
import javax.swing.WindowConstants;

public class Scan_ extends JFrame implements PlugIn, ScannerListener {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JButton jButton = null;
	private JButton jButton1 = null;
	private Scanner scanner;

	public static void main(String[] args) {
		new Scan_().setVisible(true);
	}

	public void run(String arg0) {

		new Scan_().setVisible(true);
	}

	/**
	 * This is the default constructor
	 */
	public Scan_() {
		super();
		initialize();
		try {
			scanner = Scanner.getDevice();
			scanner.addListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(300, 120);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("Scan");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {

			}
		});
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getJButton(), null);
			jContentPane.add(getJButton1(), null);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setBounds(new Rectangle(4, 16, 131, 42));
			jButton.setText("Select Device");
			jButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (scanner.isBusy() == false) {
						selectDevice();
					}

				}
			});
		}
		return jButton;
	}

	/**
	 * This method initializes jButton1
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButton1() {
		if (jButton1 == null) {
			jButton1 = new JButton();
			jButton1.setBounds(new Rectangle(160, 16, 131, 42));
			jButton1.setText("Scan");
			jButton1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {

					getScan();

				}
			});
		}
		return jButton1;
	}

	/* Select the twain source! */
	public void selectDevice() {

		try {
			scanner.select();
		} catch (ScannerIOException e1) {
			IJ.error(e1.toString());
		}

	}

	/* Get the scan! */
	public void getScan() {

		try {
			scanner.acquire();
		} catch (ScannerIOException e1) {
			IJ.showMessage("Access denied! \nTwain dialog maybe already opened!");
			// e1.printStackTrace();

		}

	}

	public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {

		if (type.equals(ScannerIOMetadata.ACQUIRED)) {

			ImagePlus imp = new ImagePlus("Scan", metadata.getImage());
			imp.show();
			metadata.setImage(null);
			try {
				new uk.co.mmscomputing.concurrent.Semaphore(0, true).tryAcquire(2000, null);
			} catch (InterruptedException e) {
				IJ.error(e.getMessage());
				// e.printStackTrace();
			}

		} else if (type.equals(ScannerIOMetadata.NEGOTIATE)) {
			ScannerDevice device = metadata.getDevice();
			try {
				device.setResolution(100);
			} catch (ScannerIOException e) {
				IJ.error(e.getMessage());
			}
			/*
			 * More options if necessary! try{
			 * device.setShowUserInterface(true);
			 * device.setShowProgressBar(true);
			 * device.setRegionOfInterest(0,0,210.0,300.0);
			 * device.setResolution(100); }catch(Exception e){
			 * e.printStackTrace(); }
			 */
		} else if (type.equals(ScannerIOMetadata.STATECHANGE)) {

			// IJ.error(metadata.getStateStr());
		} else if (type.equals(ScannerIOMetadata.EXCEPTION)) {
			IJ.error(metadata.getException().toString());

		}

	}

} // @jve:decl-index=0:visual-constraint="95,130"
