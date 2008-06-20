JAVAVERSION=1.4
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
ij.jar <- ij/**/*.java \
	icon.gif aboutja.jpg MacAdapter.class \
	IJ_Props.txt macros/*.txt
MacAdapter.class[cp plugins/MacAdapter.class .] <- plugins/MacAdapter.class

JARSIGNEROPTS=-signedjar signed-ij.jar ij.jar dscho
signed-ij.jar[jarsigner $JARSIGNEROPTS] <- ij.jar
