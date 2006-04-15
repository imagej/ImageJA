if [ -z "$1" ]; then
	echo "Need a zip file"
	exit 1
fi
case "$1" in
/*) zipfile="$1";;
*) zipfile="$(pwd)/$1";;
esac

cd "$(dirname "$0")"/../.git || exit 2
export GIT_DIR="$(pwd)"
parent=$(git-rev-parse release) || exit 3
export GIT_INDEX_FILE="$GIT_DIR/tmpIndex"
mkdir tmpCommit || exit 1
cd tmpCommit
unzip $zipfile
cd source
find -type f -print0 | xargs -0 perl $HOME/my/tools/mac2unix.pl
find -type f -print0 | xargs -0 git-update-index --add
tree=$(git-write-tree)
export GIT_AUTHOR_NAME="Wayne Rasband"
export GIT_AUTHOR_EMAIL="wsr@nih.gov"
commit=$(git-commit-tree $tree -p $parent)
git-update-ref release $commit
cd ../..
rm -rf tmpCommit tmpIndex


