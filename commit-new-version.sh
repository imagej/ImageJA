if [ -z "$1" ]; then
	echo "Need a zip file"
	exit 1
fi
case "$1" in
/*) zipfile="$1";;
*) zipfile=source/"$1";;
esac

[ -z "$(git-diff-cache -M HEAD)" ] || exit 2
git checkout -f master || exit 3
git-ls-files | while read f; do rm -f $f; done
find * -type f -exec rm {} \;
rm -f .F* .gd*
(cd ..; unzip $zipfile )
#perl $HOME/my/tools/mac2unix.pl --add-last-newline $(find * -type f)
perl $HOME/my/tools/mac2unix.pl $(find * -type f)
git-add-script $(find * -type f)
git-update-cache --refresh --remove $(git-ls-files)
GIT_AUTHOR_NAME="Wayne Rasband" GIT_AUTHOR_EMAIL="wsr@nih.gov" git commit

