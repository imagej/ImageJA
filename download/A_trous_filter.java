/*
 * A_trous_filter.java
 *
 * Created on 02/14/2005 Copyright (C) 2005 IBMP
 * ImageJ plugin
 * Version  : 1.0
 * Authors  : Olivier Marchal & jerome Mutterer
 *            written for the IBMP-CNRS Strasbourg(France)
 * Email    : olmarchal at wanadoo.fr
 *            jerome.mutterer at ibmp-ulp.u-strasbg.fr
 *
 *
 * Description : The purpose of this plugin is to remove the noise present
 * in the original image by thresholding the wavelet coefficients.
 * This plugin performs the 'a trous' wavelet transform on every size of image
 * and on a simulated image of the same size with a gaussian noise of std_dev=1.
 * We assume that the standard deviation at each scale is the product between
 * the standard deviation of the noise in the original image and the standard
 * deviation of the same scale in the simulated image. We also assume that the
 * noise should lie in the three first scales. We added an adaptative median filter
 * in order to remove "hot" or "cold" pixels (e.g. poissonian noise in CCD).
 *
 * Limitations : Only works with 8bits images and size < 1024x1024 .
 *
 * 'a trous' algorithm adapted from Image Processing and Pattern Recognition
 * by Jon Campbell and Fionn Murtagh.
 * Noise removal algorithm adapted from Multiscale Analysis Methods
 * in Astronomy and Engineering by Jean Luc Starck and Fionn Murtagh.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */


import ij.*;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.lang.Math.*;
import java.awt.*;


public class A_trous_filter implements PlugInFilter {

  ImagePlus imp;
  private byte[] save;                // save of the original image
  private double k1,k2,k3,k4, k5;    // thresholding coefficients
  private double pdev;
  private String[] nb_scale;
  private String scale;
  private double pix;
  private double pix_d[];
  private double pix_med2[];
  private double pix_s[];
  private double res[];
  private double stdev;
  private boolean poisson;
  private boolean noise;
  private double matrixp[][];
  private double matrix_big[][];
  private double matrix_big2[][];
  private double fish[];
  private String result;


  public int setup(String arg, ImagePlus imp){

    imp = WindowManager.getCurrentImage();
    return DOES_8G+DOES_8C+NO_CHANGES;
  }


  public void run(ImageProcessor ip) {

    k1=4;
    k2=3;
    k3=3;
    k4=0;
    k5=0;
    pdev=1.5;

  GenericDialog gd = new GenericDialog("A trous Wavelet filter");
  gd.addMessage("Coefficients for the different scales");
  gd.addNumericField("k1",k1,1);
  gd.addNumericField("k2",k2,1);
  gd.addNumericField("k3",k3,1);
  gd.addNumericField("k4",k4,1);
  gd.addNumericField("k5",k5,1);
  gd.addCheckbox("Non gaussian noise removal",false);
  gd.addNumericField("std dev",pdev,2);
  gd.addCheckbox("Noise display",true);
  gd.showDialog();

  if(gd.wasCanceled()){
    IJ.error("Plugin canceled");
    return;
  }


  k1=gd.getNextNumber();
  k2=gd.getNextNumber();
  k3=gd.getNextNumber();
  k4=gd.getNextNumber();
  k5=gd.getNextNumber();
  poisson = gd.getNextBoolean();
  pdev=gd.getNextNumber();
  noise=gd.getNextBoolean();

  if(gd.invalidNumber()){
    IJ.error("Invalid field");
    return;
  }


  int width = ip.getWidth();
  int height= ip.getHeight();
  int m=50;
  double matrix_big[][]=new double[width+m*2][height+m*2];
  double matrix_big2[][]=new double[width+m*2][height+m*2];
  double matrix_big_s[][]=new double[width+m*2][height+m*2];
  double matrix_big2_s[][]=new double[width+m*2][height+m*2];
  double wave3d[][][]=new double[width+m*2][height+m*2][5];
  double wave3d_s[][][]=new double[width+m*2][height+m*2][5];


// final image
ImagePlus denoised = NewImage.createByteImage("Denoised image",width,height,1,NewImage.FILL_BLACK);
ImageProcessor den_ip = denoised.getProcessor();
den_ip.copyBits(ip,0,0,Blitter.COPY);
byte[] pixels =(byte[])den_ip.getPixels();

//save original image
byte[] save= new byte[width*height];
for(int h=0;h<pixels.length;h++) save[h]=pixels[h];

// All pixels have values between 0 and 255
double[] pix_d= new double[width*height];
for(int i=0;i<pixels.length;i++){
  pix =  0xff  & pixels[i] ;
  pix_d[i] = pix;
}

stdev=noise_estimate(ip, width, height,pix_d);
//result=IJ.d2s(stdev);
// IJ.write(result);


// simulation of an image with mean=0 and std_dev=1
ImagePlus simul = NewImage.createByteImage("Simulation",width,height,1,NewImage.FILL_BLACK);
ImageProcessor simul_ip = simul.getProcessor();
simul_ip.noise(1.67) ;
byte[] pixels2 =(byte[])simul_ip.getPixels();

double[] pix_s= new double[width*height];
for(int i=0;i<pixels2.length;i++){
  pix = 0xff & pixels2[i];
  pix_s[i] = pix;
}


//initialisation of the simulated image
for(int i=0;i<width+2*m;i++)
  for(int j=0;j<height+2*m;j++)
    matrix_big_s[i][j]=0;

for(int i=0;i<width;i++)
  for(int j=0;j<height;j++)
    matrix_big_s[i+m][j+m]=pix_s[i+j*width];

mirror(matrix_big_s,width,height,m);

for(int i=0;i<width+2*m;i++)
  for(int j=0;j<height+2*m;j++)
    matrix_big2_s[i][j]=matrix_big_s[i][j];

a_trous_transform(matrix_big_s, matrix_big2_s, wave3d_s, width, height, m);



//initialisation of the image
for(int i=0;i<width+2*m;i++)
  for(int j=0;j<height+2*m;j++)
    matrix_big[i][j]=0;

for(int i=0;i<width;i++)
  for(int j=0;j<height;j++)
    matrix_big[i+m][j+m]=pix_d[i+j*width];

mirror(matrix_big,width,height,m);

for(int i=0;i<width+2*m;i++)
  for(int j=0;j<height+2*m;j++)
    matrix_big2[i][j]=matrix_big[i][j];


a_trous_transform(matrix_big, matrix_big2, wave3d, width, height, m);

denoise(wave3d, wave3d_s, width, height, m, stdev, k1, k2, k3, k4, k5);

inverse(pix_d, pixels, matrix_big2, wave3d,width, height, m);


if(poisson==true)
   filter_poisson(width,height,pdev, pix_d, pixels);


// display the denoised image
   denoised.show();
   denoised.updateAndDraw();

//display removed noise
if(noise==true)
  disp_noise(width, height, pix_d, save);


} //run





// filter that acts like a filter median when the value of central pixel
// is superior or inferior to the mean +/- dev_std of the 3x3 neighborhood
public void filter_poisson(int width, int height, double devp, double[] pix_d, byte[] pixels){

int pos;
double dev;
double mean;
double[][] tmp_mat = new double[width+2][height+2]; //temporary matrix for edge effets
double[] fish = new double[9];
double[] fish_save = new double[9];
double[] tmp =new double[8];
double mat[][]=new double[width][height];

     for(int i=0;i<width;i++){
       for(int j=0;j<height;j++){
          mat[i][j]=pix_d[i+j*width];
        }
      }


  for(int i=0;i<width+2;i++)
    for(int j=0;j<height+2;j++)
      tmp_mat[i][j]=0;             // fill the tmp matrix with zeros

  for(int i=0;i<width;i++)
    for(int j=0;j<height;j++)
      tmp_mat[i+1][j+1]=mat[i][j];  // copy the matrix in the center of the tmp matrix

  for(int i=1;i<=width;i++){          //scan of the matrix
    for(int j=1;j<=height;j++){

      pos=0;
      for(int x=i-1;x<=i+1;x++){
        for(int y=j-1;y<=j+1;y++){    // copy of the 3x3 neighborhood
           fish[pos]=tmp_mat[x][y];
            pos++;
	}
      }

  for(int z=1;z<fish.length;z++)  fish_save[z]=fish[z];

  sort_array(fish_save,9);

  mean=0;
  dev=0;
  for(int k=0;k<tmp.length;k++) {tmp[k]=fish[k]; mean+=tmp[k];}
  mean=mean/tmp.length;
  dev=stdev_calc(tmp);
  if(fish[4]>mean+devp*dev || fish[4]<mean-devp*dev) tmp_mat[i][j]=fish_save[4];

    }
}

for(int i=0;i<width;i++)
  for(int j=0;j<height;j++)
    mat[i][j]=tmp_mat[i+1][j+1];  // copy of the tmp matrix in the final matrix

for(int i=0;i<width;i++){
      for(int j=0;j<height;j++){
         pix_d[i+j*width]=mat[i][j];
       }
     }

  for(int f=0;f<height*width;f++)                   // matrix update
      pixels[f]= (byte)pix_d[f];


}//poisson filter




// calculate the standard deviation of an array
  public double stdev_calc(double[] tab){

    double mean_t=0;
    double sigma_t=0;
    double stdev_t=0;

    for(int m=0;m<tab.length;m++) {mean_t+=tab[m];}
    mean_t=mean_t/tab.length;

    for(int n=0;n<tab.length;n++) {sigma_t+=(tab[n]-mean_t)*(tab[n]-mean_t);}
    sigma_t=sigma_t/tab.length;
    stdev_t=Math.sqrt(sigma_t);

   return stdev_t;

}//stdev_calc


// calculate the standard deviation of a matrix
  public void stdev_calc_m(double[][][] tab, double[] res, int width, int height, int m){

    double mean_t=0;
    double sigma_t=0;
    double stdev_t=0;
    double size = width*height;

for(int k=0; k<5; k++){


    for(int i=m;i<width+m;i++)
      for(int j=m;j<height+m;j++)
          mean_t+=tab[i][j][k];
        //mean_t+=Math.abs(tab[i][j][k]);

      mean_t=mean_t/(size);

      for(int i=m;i<width+m;i++)
        for(int j=m;j<height+m;j++)
          sigma_t+=(tab[i][j][k]-mean_t)*(tab[i][j][k]-mean_t);

    sigma_t=sigma_t/(size);
    stdev_t=Math.sqrt(sigma_t);
    res[k]=stdev_t;

}

}//stdev_calc



// sort an array of doubles
public void sort_array(double array[], int size){

int i,j;
double  tmp1,tmp2;

 for(i=0;i<=size-2;i++){
   for(j=i+1;j<=size-1;j++){
      if (array[i]>=array[j]){
       tmp1=array[i];
       tmp2=array[j];
       array[j]=tmp1;
       array[i]=tmp2;
     }
  }
}

}//sort


public void mirror(double[][] matrix_big, int width, int height, int m){


//bottom
       for(int i=0;i<width;i++){
              for(int j=1;j<=m;j++){
                 matrix_big[i+m][m-1+height+j]=matrix_big[i+m][m-1+height-j];
               }
        }

//top
      for(int i=0;i<width;i++){
        for(int j=1;j<=m;j++){
          matrix_big[i+m][m-j]=matrix_big[i+m][m+j];
                    }
             }


    //right
     for(int i=0;i<=m;i++){
      for(int j=0;j<height;j++){
        matrix_big[m-1+width+i][m+j]=matrix_big[m-1+width-i][m+j];
                   }
           }

    //left
     for(int i=0;i<=m;i++){
      for(int j=0;j<height;j++){
        matrix_big[m-i][m+j]=matrix_big[m+i][m+j];
                   }
         }



   //top left corner
   for(int i=0;i<=m;i++){
    for(int j=0;j<=m;j++){
      matrix_big[i][j]=matrix_big[2*m-i][2*m-j];
                 }
       }

//bottom right corner
  for(int i=0;i<=m;i++){
   for(int j=0;j<=m;j++){
     matrix_big[i+width+m-1][j+height+m-1]=matrix_big[width+m-1-i][height+m-1-j];
                }
      }


//top right corner
  for(int i=0;i<=m;i++){
   for(int j=0;j<=m;j++){
     matrix_big[i+width+m-1][j]=matrix_big[m-1+width-i][m*2-j];
                }
      }

//bottom left corner
  for(int i=0;i<=m;i++){
   for(int j=0;j<=m;j++){
     matrix_big[i][j+height+m-1]=matrix_big[2*m-i][height+m-1-j];
                }
      }

}//mirror


// estimate the noise in the input image
 public double noise_estimate(ImageProcessor ip,int width,int height, double[] pix_d){

double stdev;

 ImagePlus med = NewImage.createByteImage("med image",width,height,1,NewImage.FILL_BLACK);
 ImageProcessor med_ip = med.getProcessor();
 med_ip.copyBits(ip,0,0,Blitter.COPY);
 byte[] pix_med =(byte[])med_ip.getPixels();

  med_ip.medianFilter();
  double[] pix_med2= new double[width*height];
   for(int i=0;i<pix_med.length;i++){
      pix =  0xff  & pix_med[i] ;
      pix_med2[i] = pix;
    }

  stdev=0;
  double[] res= new double[width*height];
    for(int i=0;i<width*height;i++){
          res[i]=pix_d[i]-pix_med2[i];
          if(res[i]<0) res[i]=0;
          if(res[i]>255) res[i]=255;
    }

  stdev=stdev_calc(res);
return stdev;



 }


// convolution between the image and a 2D B3-spline function
public void convolution(double[][] mat1, double[][]mat2, int p, int q, int width, int height, int m){

    for(int i=m;i<width+m;i++){
      for(int j=m;j<height+m;j++){

           mat2[i][j]=(mat1[i-q][j-q]/256 + mat1[i-q][j-p]/64 + mat1[i-q][j]*3/128 + mat1[i-q][j+1]/64 + mat1[i-q][j+q]/256 +
                mat1[i-p][j-q]/64 + mat1[i-p][j-p]/16 + mat1[i-p][j]*3/32 + mat1[i-p][j+p]/16 + mat1[i-p][j+q]/64 +
                mat1[i][j-q]*3/128 + mat1[i][j-p]*3/32 + mat1[i][j]*9/64 + mat1[i][j+p]/32 + mat1[i][j+q]*3/128 +
                mat1[i+p][j-q]/64 + mat1[i+p][j-p]/16 + mat1[i+p][j]*3/32 + mat1[i+p][j+p]/16 + mat1[i+p][j+q]/64 +
                mat1[i+q][j-q]/256 + mat1[i+q][j-p]/64 + mat1[i+q][j]*3/128 + mat1[i+q][j+p]/64 + mat1[i+p][j+q]/256);

       }
    }

  }//convolution


//remove the noise
public void denoise(double[][][] wave, double[][][] wave_s, int width, int height,int m, double stddev, double k1, double k2, double k3, double k4, double k5){

   double[] dev_s= new double[5];

   stdev_calc_m(wave_s, dev_s, width, height, m);

 /*  for(int i=0;i<5;i++){
     result=IJ.d2s(dev_s[i]);
     IJ.write(result);
   }*/

    for(int i=0;i<width+m*2;i++){
     for(int j=0;j<height+m*2;j++){

        if( Math.abs(wave[i][j][0]) < k1*stddev*dev_s[0] ) wave[i][j][0]=0;
        if( Math.abs(wave[i][j][1]) < k2*stddev*dev_s[1] ) wave[i][j][1]=0;
        if( Math.abs(wave[i][j][2]) < k3*stddev*dev_s[2] ) wave[i][j][2]=0;
        if( Math.abs(wave[i][j][3]) < k4*stddev*dev_s[3] ) wave[i][j][3]=0;
        if( Math.abs(wave[i][j][4]) < k5*stddev*dev_s[4] ) wave[i][j][4]=0;

     }
     }

  }//denoise


//perform the a trous wavelet transform
public void a_trous_transform( double[][] matrix_big, double[][] matrix_big2, double[][][] wave3d, int width, int height, int m){

 double matrix_tmp[][]=new double[width+m*2][height+m*2];
 double wave_t[][]=new double[width+m*2][height+m*2];
 int distance[]= {1,2,4,8,16,32,64};

    for(int i=0;i<width;i++)
      for(int j=0;j<height;j++)
        wave_t[i+m][j+m]=0;



    for(int k=0;k<5;k++){

 convolution(matrix_big, matrix_big2,  distance[k],  distance[k+1],  width,  height,  m);


  for(int i=0;i<width+m*2;i++)
    for(int j=0;j<height+m*2;j++)
      wave_t[i][j]=matrix_big[i][j]-matrix_big2[i][j];


  for(int i=0;i<width+m*2;i++)
    for(int j=0;j<height+m*2;j++)
      wave3d[i][j][k]=wave_t[i][j];


   for(int i=0;i<width+m*2;i++)
       for(int j=0;j<height+m*2;j++)
          matrix_tmp[i][j]=0;

      for(int i=m;i<width+m;i++)
       for(int j=m;j<height+m;j++)
          matrix_tmp[i][j]=matrix_big2[i][j];

   mirror(matrix_tmp,width,height,m);

   for(int i=0;i<width+m*2;i++)
       for(int j=0;j<height+m*2;j++)
          matrix_big2[i][j]=matrix_tmp[i][j];

      for(int i=0;i<width+m*2;i++)
       for(int j=0;j<height+m*2;j++)
          matrix_big[i][j]=matrix_big2[i][j];
  }

  }// a_trous


//inverse transform
public void inverse(double[] pix_d, byte[] pixels, double[][] matrix_big2, double[][][] wave3d, int width, int height, int m){

    double res[][]=new double[width+m*2][height+m*2];
    double res_petit[][]=new double[width][height];


 for(int i=0;i<width+m*2;i++)
    for(int j=0;j<height+m*2;j++)
       res[i][j]=matrix_big2[i][j]+wave3d[i][j][0]+wave3d[i][j][1]+wave3d[i][j][2]+wave3d[i][j][3]+wave3d[i][j][4];


           for(int i=0;i<width;i++){
             for(int j=0;j<height;j++){
               res_petit[i][j]=res[i+m][j+m];
              }
           }

           for(int i=0;i<width;i++){
             for(int j=0;j<height;j++){
               pix_d[i+j*(width)]=res_petit[i][j];
              }
           }

          for(int i=0;i<(width)*(height);i++){

             if (pix_d[i] < 0) pix_d[i]  = 0;
             if ( pix_d[i] > 255) pix_d[i]  = 255;

          }


           for(int i=0;i<width*height;i++)
               pixels[i]=(byte)pix_d[i];


  }//inverse




public void disp_noise(int width, int height, double[] pix_d, byte[] save){

  ImagePlus noise = NewImage.createByteImage("Removed noise",width,height,1,NewImage.FILL_BLACK);
  ImageProcessor n_ip = noise.getProcessor();
  byte[] rem_noise =(byte[])n_ip.getPixels();
  double[] rem_noise_d= new double[width*height];
    for(int i=0;i<rem_noise.length;i++){
      pix =  0xff  & rem_noise[i] ;
      rem_noise_d[i] = pix;
    }

  double[] save_d= new double[width*height];
    for(int i=0;i<save.length;i++){
      pix =  0xff  & save[i] ;
      save_d[i] = pix;
    }

  for(int d=0;d<width*height;d++) rem_noise_d[d]=save_d[d]-pix_d[d];

  for(int f=0;f<width*height;f++){
    if(rem_noise_d[f]<0) rem_noise_d[f]=0;
    if(rem_noise_d[f]>255) rem_noise_d[f]=255;
  }

  for(int d=0;d<width*height;d++) rem_noise[d]=(byte)rem_noise_d[d];

  noise.show();
  noise.updateAndDraw();
  IJ.run("3-3-2 RGB");

  }


  }//class
