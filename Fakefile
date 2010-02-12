JAVAVERSION=1.4
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
ij.jar <- ij/**/*.java com/apple/**/*.java javax/script/*.java \
	icon.gif[images/icon.gif] aboutja.jpg[images/aboutja.jpg] plugins/*.java \
	plugins/JavaScriptEvaluator.class plugins/MacAdapter.class \
	IJ_Props.txt macros/*.txt

signed-ij.jar[./make-signed-jar.sh] <- ij.jar
