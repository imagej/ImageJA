#!/bin/sh

BRANCHNAME=imagej
URL=git@github.com:imagej/ImageJA

git rev-parse $BRANCHNAME >/dev/null 2>/dev/null ||
git fetch $URL $BRANCHNAME:refs/heads/$BRANCHNAME || {
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

if test -t 0
then
	MESSAGE="$(unzip -p "$zipfile" source/release-notes.html |
		sed -e 's/^.*<body>/<ul>/' -e 's/<a href[^>]*>Home<\/a>//' |
		w3m -cols 72 -dump -T text/html |
		sed -e '/^Home/d' |
		git stripspace)"
else
	MESSAGE="$(cat)"
fi

MAC2UNIX="$(cd "$(dirname "$0")" && pwd)"/mac2unix.pl

git fetch $URL $BRANCHNAME &&
git push . +FETCH_HEAD:$BRANCHNAME || {
	echo "Could not update $BRANCHNAME to $UPSTREAM"
	exit 1
}
parent=$(git rev-parse $BRANCHNAME) || {
	echo "Could not get revision for $BRANCHNAME"
	exit 1
}
ONELINE="$(git cat-file commit $BRANCHNAME)" || {
	echo "Could not get commit message for $BRANCHNAME"
	exit 1
}
ONELINE="$(echo "$ONELINE" | sed '1,/^$/d' | sed '2q')"
case "$MESSAGE" in
"$ONELINE"*)
	echo "Already have a commit starting with $ONELINE"
	exit 1
	;;
esac
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
commit=$(echo "$MESSAGE" | git commit-tree $tree -p $parent) &&
git update-ref refs/heads/$BRANCHNAME $commit &&
cd ../.. &&
rm -rf tmpCommit tmpIndex
