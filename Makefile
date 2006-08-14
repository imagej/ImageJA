JAVAS=$(wildcard ij/*.java ij/*/*.java ij/*/*/*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
ALLCLASSES=ij/*.class ij/*/*.class ij/*/*/*.class
COPYFILES=icon.gif aboutja.jpg MacAdapter.class
TEXTFILES=IJ_Props.txt $(wildcard macros/*.txt)

ifeq ($(uname_O),Cygwin)
PLUGINSHOME=$(shell cygpath --mixed $(shell pwd))
CPSEP=\;
else
PLUGINSHOME=$(shell pwd)
CPSEP=:
endif
CLASSPATH=$(JAVA_HOME)/lib/tools.jar$(CPSEP)$(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP).
JAVACOPTS=-O -classpath $(CLASSPATH) -source 1.3 -target 1.3

ij.jar: $(COPYFILES) $(CLASSES) $(TEXTFILES)
	jar cvmf MANIFEST.MF ij.jar $(COPYFILES) $(ALLCLASSES) $(TEXTFILES)

signed-ij.jar:
	jarsigner -signedjar signed-ij.jar $(shell cat .jarsignerrc) ij.jar dscho

icon.gif aboutja.jpg: %: images/%
	cp $< $@

MacAdapter.class: plugins/MacAdapter.class
	cp $< $@

%.class: %.java
	javac $(JAVACOPTS) "$^"

clean:
	rm $(COPYFILES) $(ALLCLASSES)

