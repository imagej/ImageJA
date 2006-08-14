JAVAS=$(wildcard ij/*.java ij/*/*.java ij/*/*/*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))

ifeq ($(uname_O),Cygwin)
PLUGINSHOME=$(shell cygpath --mixed $(shell pwd))
CPSEP=\;
else
PLUGINSHOME=$(shell pwd)
CPSEP=:
endif
CLASSPATH=$(JAVA_HOME)/lib/tools.jar$(CPSEP)$(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP).
JAVACOPTS=-O -classpath $(CLASSPATH) -source 1.3 -target 1.3

jar: IJ_Props.txt icon.gif aboutja.jpg MacAdapter.class $(CLASSES) macros/*.txt
	jar cvmf MANIFEST.MF ij.jar $^

icon.gif aboutja.jpg: %: images/%
	cp $< $@

MacAdapter.class: plugins/MacAdapter.class
	cp $< $@

%.class: %.java
	javac $(JAVACOPTS) "$^"

