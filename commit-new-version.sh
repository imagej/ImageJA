#!/bin/sh

BRANCHNAME=imagej
UPSTREAM=origin

git rev-parse $BRANCHNAME >/dev/null 2>/dev/null || {
	echo "No branch $BRANCHNAME yet!"
	exit 1
}

test $# = 1 || {
	echo "Need a .zip file with the source" >&2
	exit 1
}

case "$1" in
/*) zipfile="$1";;
*) zipfile="$(pwd)/$1";;
esac

test -t 0 && {
	unzip -p "$zipfile" source/release-notes.html |
	sed -e 's/^.*<body>/<ul>/' -e 's/<a href[^>]*>Home<\/a>//' |
	w3m -cols 72 -dump -T text/html |
	sed -e '/^Home/d' |
	git stripspace |
	sh "$0" "$@"
	exit
}

MAC2UNIX="$(cd "$(dirname "$0")" && pwd)"/mac2unix.pl

git fetch $UPSTREAM $BRANCHNAME &&
git push . FETCH_HEAD:$BRANCHNAME || {
	echo "Could not update $BRANCHNAME to $UPSTREAM"
	exit 1
}
parent=$(git rev-parse $BRANCHNAME) || {
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
if !  find \( -name .DS_Store -name .FBCIndex -o -name .FBCLockFolder -o -name .gdb_history \) -exec rm -rf {} \;
then
	echo "No temporary files removed" >&2
fi &&
find -type f -print0 | xargs -0 perl "$MAC2UNIX" &&
find -type f -print0 | xargs -0 git update-index --add &&
tree=$(git write-tree) &&
export GIT_AUTHOR_NAME="Wayne Rasband" &&
export GIT_AUTHOR_EMAIL="wsr@nih.gov" &&
commit=$(git commit-tree $tree -p $parent) &&
git update-ref refs/heads/$BRANCHNAME $commit &&
cd ../.. &&
rm -rf tmpCommit tmpIndex
