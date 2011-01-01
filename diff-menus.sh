#!/bin/sh

cd "$(dirname "$0")"/..

cat ij/Menus.java | sed -n -e '/^[ 	]*addPlug/s/([^,]*, /(/p' > .git/m1
cat headless/ij/Menus.java | sed -n -e '/^[ 	]*addPlug/p' > .git/m2

git diff --no-index -U1 .git/m2 .git/m1
