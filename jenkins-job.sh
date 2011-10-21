#!/bin/sh

#test $# != 1 && {
#	echo "Need to get the token as parameter" >&2
#	exit 1
#}
test -z "$HUDSON_URL" && {
	echo "Need to be run from Jenkins" >&2
	exit 1
}

URL=http://imagej.nih.gov/ij

TIMESTAMP=.git/ij.timestamp
curl --silent --head $URL/notes.html |
grep -i "^Last-Modified:" > $TIMESTAMP.new
test -s $TIMESTAMP.new &&
test -f $TIMESTAMP &&
test "$(cat $TIMESTAMP.new)" = "$(cat $TIMESTAMP)" &&
exit 0

SRC_URL=$URL/download/src
NOTES_URL=$URL/notes.html
VERSION="$(curl $SRC_URL/ | \
	sed -n "s/^.*ij\([0-9a-z]*\)-src.zip.*$/\1/p" | \
	tail -n 1)"
DOTVERSION=$(echo $VERSION | sed "s/^./&./")
git log $BRANCHNAME | grep "^      • $DOTVERSION,\? " && {
	echo "Already have $DOTVERSION"
	exit 0
}
ZIP=ij$VERSION-src.zip
test -f $ZIP || curl $SRC_URL/$ZIP > .git/$ZIP || {
	echo "Could not get $SRC_URL/$ZIP"
	exit 1
}
NOTES=.git/notes$VERSION.txt
test -f $NOTES || w3m -cols 72 -dump $NOTES_URL >$NOTES || {
	echo "Could not get notes"
	exit 1
}
(cat $NOTES | \
 sed -n \
	-e "s/^  • $DOTVERSION,\? /•&/" \
	-e "/^•  • $DOTVERSION,\? /,\$p" |
 sed \
	-e "/^  • /,\$d" \
	-e "s/^•\(  • $DOTVERSION,\? \)/\1/" |
 sh -x commit-new-version.sh .git/$ZIP) || {
	echo "Could not commit!"
	exit 1
}
# TODO: push
mv $TIMESTAMP.new $TIMESTAMP
