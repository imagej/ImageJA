if [ -z "$1" ]; then
	echo "Need a zip file"
	exit 1
fi
case "$1" in
/*) zipfile="$1";;
*) zipfile=source/"$1";;
esac

[ -z "$(git-diff-index -M HEAD)" ] || exit 2
git checkout master || exit 3
git-ls-files | while read f; do rm -f $f; done
(cd ..; unzip -o $zipfile )
(cd ..; unzip -v $zipfile ) | \
	sed -n "s/^.* source\///p" | \
	grep -ve "^$" -e "/$" | \
	while read f; do
		#perl $HOME/my/tools/mac2unix.pl --add-last-newline "$f"
		perl $HOME/my/tools/mac2unix.pl "$f"
		echo "Adding $f"
		git-add "$f"
	done
git-ls-files | xargs git-update-index --refresh --remove 
GIT_AUTHOR_NAME="Wayne Rasband" GIT_AUTHOR_EMAIL="wsr@nih.gov" git commit

