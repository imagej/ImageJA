javaVersion=1.5
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
EXCLUDE(ij.jar)=headless/**/*
ij.jar <- **/*.java \
	icon.gif[images/icon.gif] aboutja.jpg[images/aboutja.jpg] \
	IJ_Props.txt macros/*.txt

signed-ij.jar[./make-signed-jar.sh] <- ij.jar

CLASSPATH(headless.jar)=ij.jar
headless.jar <- headless/**/*
