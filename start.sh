#!/bin/sh

# copy this wrapper into the ImageJ directory.

IMAGEJHOME="$(cd "$(dirname "$0")"; pwd)"
IMAGEJHOME=/Users/gene099/my/ImageJ

if [ ! -f "$IMAGEJHOME"/ij.jar -a -f "$IMAGEJHOME"/../ij.jar ]; then
	IMAGEJHOME="$IMAGEJHOME"/..
fi

JAVA=java

if [ -z "$JAVA_HOME" ]; then
	for d in /usr/local/jdk1.5.0_03 /usr/local/j2sdk1.4.2 /usr/lib/j2se/1.4; do
		if [ -d "$d" ]; then
			JAVA_HOME="$d"
			JAVA=$JAVA_HOME/bin/java
			MOREJARS="$(ls "$JAVA_HOME"/lib/*.jar|tr "\012" ":")"
			break
		fi
	done
fi

#cd "$IMAGEJHOME"

$JAVA -Xmx512m -cp ${MOREJARS}./ij.jar -Dplugins.dir="$IMAGEJHOME" ij.ImageJ "$@"

