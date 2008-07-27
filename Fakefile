JAVAVERSION=1.4
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
ij.jar <- ij/**/*.java \
	icon.gif aboutja.jpg MacAdapter.class \
	IJ_Props.txt macros/*.txt
icon.gif[cp $PRE .] <- images/icon.gif
aboutja.jpg[cp $PRE .] <- images/aboutja.jpg
MacAdapter.class[cp $PRE .] <- plugins/MacAdapter.class

JARSIGNEROPTS=-signedjar signed-ij.jar ij.jar dscho
signed-ij.jar[jarsigner $JARSIGNEROPTS] <- ij.jar
