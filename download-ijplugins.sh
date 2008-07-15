#!/bin/sh

BASE_URL=http://rsb.info.nih.gov/ij/plugins
BRANCH=refs/heads/ijplugins
VERBOSE=t

NEED_TO_PUSH=
download_plugin () {
	FILE="$1"
	OPT=
	test -f "$FILE" && OPT="--time-cond $FILE"
	CONTENTS="$(eval curl --silent $OPT $BASE_URL/$FILE)"
	test -z "$CONTENTS" && return
	echo "$CONTENTS" > "$FILE"
	FILES="$(echo "$CONTENTS" |
	  sed -n '/Source:/,/Description:/s/.*a href="\([^"]*\)".*/\1/pi' |
	  sort | uniq)"
	test -z "$FILES" && return
	test -z "$VERBOSE" || echo "Downloading $FILE" >&2

	GIT_INDEX_FILE=.index
	GIT_AUTHOR_NAME="Plugin Downloader"
	GIT_AUTHOR_EMAIL="plugin.downloader@fiji"
	GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME"
	GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"
	export GIT_INDEX_FILE GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL \
		GIT_COMMITTER_NAME GIT_COMMITTER_EMAIL

	PARENT=$(git rev-parse --verify $BRANCH 2> /dev/null)
	test -z "$PARENT" || PARENT="-p $PARENT"

	git read-tree $BRANCH 2> /dev/null
	for f in $FILES
	do
		test -z "$VERBOSE" || echo "Getting $f" >&2
		sha1=$(curl --silent $BASE_URL/$f |
			git hash-object -w --stdin) || break
		printf "100644 $sha1 0\t$f\n"
	done | git update-index --index-info &&
	git diff-index --cached --quiet HEAD && return
	sha1=$(echo "$CONTENTS" | w3m -cols 72 -dump -T text/html |
		eval git commit-tree $(git write-tree) $PARENT) &&
	git update-ref $BRANCH $sha1
	NEED_TO_PUSH=t
}

cd "$(dirname "$0")"/../.git &&
GIT_DIR="$(pwd)" &&
export GIT_DIR &&
mkdir -p ijplugins &&
cd ijplugins &&
for plugin in $(curl --silent $BASE_URL/ |
	sed -n 's/.*a href="\([^\/]*.html\)".*/\1/pi')
do
	download_plugin $plugin || break
done &&
(test -z "$NEED_TO_PUSH" || git push orcz $BRANCH)
