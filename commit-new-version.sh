#!/bin/sh

BRANCHNAME=imagej
UPSTREAM=orcz

git rev-parse $BRANCHNAME >/dev/null 2>/dev/null || {
	echo "No branch $BRANCHNAME yet!"
	exit 1
}

if [ -z "$1" ]; then
	URL=http://rsb.info.nih.gov/ij
	SRC_URL=$URL/download/src
	NOTES_URL=$URL/notes.html
	VERSION="$(curl $SRC_URL/ | \
		sed -n "s/^.*ij\([0-9a-z]*\)-src.zip.*$/\1/p" | \
		tail -n 1)"
	DOTVERSION=$(echo $VERSION | sed "s/^./&./")
	git log $BRANCHNAME | grep "^      • $DOTVERSION, " && {
		echo "Already have $DOTVERSION"
		exit 0
	}
	ZIP=ij$VERSION-src.zip
	test -f $ZIP || curl $SRC_URL/$ZIP > $ZIP || {
		echo "Could not get $SRC_URL/$ZIP"
		exit 1
	}
	NOTES=notes$VERSION.txt
	test -f $NOTES || w3m -dump $NOTES_URL >$NOTES || {
		echo "Could not get notes"
		exit 1
	}
	(cat $NOTES | \
	 sed -n \
		-e "s/^  • $DOTVERSION, /•&/" \
		-e "/^•  • $DOTVERSION, /,\$p" |
	 sed \
		-e "/^  • /,\$d" \
		-e "s/^•\(  • $DOTVERSION, \)/\1/" |
	 sh "$0" $ZIP) || {
		echo "Could not commit!"
		exit 1
	}
	exit 0
fi

case "$1" in
/*) zipfile="$1";;
*) zipfile="$(pwd)/$1";;
esac

MAC2UNIX="$(cd "$(dirname "$0")" && pwd)"/mac2unix.pl

git fetch $UPSTREAM $BRANCHNAME &&
git push . FETCH_HEAD:$BRANCHNAME || {
	echo "Could not update $BRANCHNAME to $UPSTREAM"
	exit 1
}
parent=$(git-rev-parse $BRANCHNAME) || {
	echo "Could not get revision for $BRANCHNAME"
	exit 1
}
export GIT_DIR="$(cd "$(git rev-parse --git-dir)" && pwd)"
export GIT_INDEX_FILE="$(pwd)/tmpIndex"
mkdir tmpCommit || {
	echo "tmpCommit/ already exists?"
	exit 1
}
cd tmpCommit &&
unzip $zipfile &&
cd source &&
export GIT_WORK_TREE="$(pwd)" &&
find -type f -print0 | xargs -0 perl "$MAC2UNIX" &&
find -type f -print0 | xargs -0 git-update-index --add &&
tree=$(git-write-tree) &&
export GIT_AUTHOR_NAME="Wayne Rasband" &&
export GIT_AUTHOR_EMAIL="wsr@nih.gov" &&
commit=$(git-commit-tree $tree -p $parent) &&
git-update-ref refs/heads/$BRANCHNAME $commit &&
cd ../.. &&
rm -rf tmpCommit tmpIndex
