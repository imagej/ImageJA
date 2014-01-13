#!/bin/sh

URL=git://github.com/imagej/ImageJA
BRANCH=refs/heads/master

grep Sync .git/hooks/post-merge >/dev/null 2>/dev/null || {
	echo "post-merge hook not installed properly!"
	PWD="$(cd "$(dirname "$0")"; pwd)"
	echo "Try 'ln -s $PWD/post-merge .git/hooks/post-merge'"
	exit 1
}

test $BRANCH != "$(git symbolic-ref HEAD)" &&
! git checkout ${BRANCH##refs/heads/} && {
	echo "Must be on branch $BRANCH"
	exit 1
}

git fetch $URL $BRANCH || {
	echo "No branch orcz?"
	exit 1
}

test $(git rev-parse $BRANCH) != $(git rev-parse FETCH_HEAD) && {
	echo "Branch $BRANCH is not up-to-date!"
	exit 1
}

git merge imagej || {
	VERSION="$(git log -1 --pretty=format:%s HEAD^2 imagej |
		sed -n "s/^[^0-9]*\([\\.0-9]*.\),.*$/\1/p")"
	echo "Sync with ImageJ $VERSION" > .git/MERGE_MSG
	exit 1
}
