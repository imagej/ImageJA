#!/bin/sh

URL=git://repo.or.cz/imageja.git
BRANCH=refs/heads/imageja

grep Sync .git/hooks/post-merge >/dev/null 2>/dev/null || {
	echo "post-merge hook not installed properly!"
	PWD="$(cd "$(dirname "$0")"; pwd)"
	echo "Try 'ln -s $PWD/post-merge .git/hooks/post-merge"
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

git merge imagej

