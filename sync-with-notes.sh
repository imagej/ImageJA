#!/bin/sh

#test $# != 1 && {
#	echo "Need to get the token as parameter" >&2
#	exit 1
#}
test -z "$HUDSON_URL" && {
	echo "Need to be run from Jenkins" >&2
	exit 1
}

URL=https://wsr.imagej.net
SRC_URL=$URL/src
NOTES_URL=$URL/notes.html

VERSION="$(curl $SRC_URL/ | \
	sed -n "s/^.*ij\([0-9a-z]*\)-src.zip.*$/\1/p" | \
	tail -n 1)"
test "$VERSION" || {
	echo "Could not extract version from $SRC_URL" >&2
	exit 1
}
DOTVERSION=$(echo $VERSION | sed "s/^./&./")
git log | grep "^      .[^ ]\? $DOTVERSION,\? " && {
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

# make sure that the sed call succeeds to extract the commit message
LANG=en_US.utf-8
export LANG

(cat $NOTES | \
 sed -n \
	-e "s/^  [^ ] $DOTVERSION,\? /•&/" \
	-e "/^[^ ]  [^ ] $DOTVERSION,\? /,\$p" |
 sed \
	-e "/^  [^ ] /,\$d" \
	-e "/^Version/,\$d" \
	-e "s/^[^ ]\(  [^ ] $DOTVERSION,\? \)/\1/" |
 sh -x "$(dirname "$0")"/commit-new-version.sh .git/$ZIP) || {
	echo "Could not commit!"
	exit 1
}
sh -x "$(dirname "$0")"/sync-with-imagej.sh || {
	echo "Could not update 'master'"
	exit 1
}
test -n "$NO_PUSH" && exit

for REMOTE in \
	git@github.com:imagej/ImageJA
do
	ERR="$(git push $REMOTE imagej master "v$DOTVERSION" 2>&1 &&
		cd tools &&
		git push $REMOTE tools 2>&1)" ||
	case "$REMOTE $ERR" in
	*repo.or.cz*"Connection refused"*)
		echo "Warning: repo.or.cz was not reachable"
		;;
	*)
		echo "${ERR}Could not push"
		exit 1
		;;
	esac
done
