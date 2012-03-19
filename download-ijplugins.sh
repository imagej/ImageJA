#!/bin/sh

BASE_URL=http://rsb.info.nih.gov/ij/plugins
BRANCH=refs/heads/ijplugins
VERBOSE=t

NEED_TO_PUSH=
download_plugin () {
	GIT_INDEX_FILE=.git/.index
	export GIT_INDEX_FILE
	FILE="$1"
	OPT=
	case "$FILE" in */*) mkdir -p "$(dirname "$FILE")";; esac
	test -f "$FILE" && OPT="--time-cond $FILE"
	CONTENTS="$(eval curl --silent $OPT $BASE_URL/$FILE)"
	if test -z "$CONTENTS"
	then
		if git rev-parse $BRANCH:$FILE > /dev/null 2>&1 &&
			git diff --quiet "$FILE"
		then
			return
		fi
		CONTENTS="$(cat "$FILE")"
	else
		echo "$CONTENTS" > "$FILE"
	fi
	FILES="$(echo "$CONTENTS" | tr '\r' '\n' |
	  sed -n '/Source:/,/Description:/s/.*a href="\([^"]*\)".*/\1/pi' |
	  sort | uniq)"
	test -z "$FILES" && return
	test -z "$VERBOSE" || echo "Downloading $FILE" >&2

	GIT_AUTHOR_NAME="Plugin Downloader"
	GIT_AUTHOR_EMAIL="plugin.downloader@fiji"
	GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME"
	GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"
	export GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL \
		GIT_COMMITTER_NAME GIT_COMMITTER_EMAIL

	PARENT=$(git rev-parse --verify $BRANCH 2> /dev/null)
	test -z "$PARENT" || PARENT="-p $PARENT"

	git read-tree $BRANCH 2> /dev/null
	git add "$FILE" &&
	echo "$FILES" |
	while read f
	do
		case "$f" in
		"http://"*)
			url=$f
			f="${f##http://rsb.info.nih.gov/ij/plugins/}"
			;;
		*)
			url=$(dirname $BASE_URL/$FILE)/$f
			;;
		esac
		test -z "$VERBOSE" || echo "Getting $f" >&2
		sha1=$(curl --silent $(echo "$url" | sed "s/ /%20/g") |
			git hash-object -w --stdin) || break
		git update-index --add --cacheinfo 100644 $sha1 "$f" || break
	done &&
	git diff-index --cached --quiet $BRANCH && return
	sha1=$(echo "$CONTENTS" |
		w3m -cols 72 -dump -T text/html |
		sed -e 1N -e '/^home | /d' |
		eval git commit-tree $(git write-tree) $PARENT) &&
	git update-ref $BRANCH $sha1
	NEED_TO_PUSH=t
}

mkdir -p ijplugins &&
cd ijplugins &&
if ! test -d .git
then
	git init &&
	git remote add -t $BRANCH -f origin $URL &&
	git checkout $BRANCH
fi &&
if test $# = 0
then
	for plugin in $(curl --silent $BASE_URL/ |
		sed -n 's/.*a href="\([^:]*.html\)".*/\1/pi')
	do
		download_plugin $plugin || break
	done &&
	(test -z "$NEED_TO_PUSH" || git push orcz $BRANCH)
else
	for plugin in "$@"
	do
		download_plugin $plugin || break
	done ||
	echo "Failure"
fi
