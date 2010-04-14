JAVAVERSION=1.4
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
ij.jar <- **/*.java \
	icon.gif[images/icon.gif] aboutja.jpg[images/aboutja.jpg] \
	IJ_Props.txt macros/*.txt

signed-ij.jar[./make-signed-jar.sh] <- ij.jar
