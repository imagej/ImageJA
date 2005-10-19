#!/bin/bash

function readLink () {
	ls -l "$@" | sed "s/^.*-> //"
}

if [ -z "$1" ]; then
	waitFor=""
else
	waitFor="$(basename "$(readLink "$1")")"
fi

for i in .git/refs/heads/*; do
	name=$(basename $i)
	if [ $name = "$waitFor" ]; then
		echo "found $waitFor"
		waitFor=""
	fi
	if [ -z "$waitFor" -a $name != cumul -a $name != master -a $name != tools -a $name != imageja ]; then
		echo "Compiling $name"
		git checkout $name || exit 1
		#sh .git/cleanup.sh || exit 2
		ant build || exit 3
	fi
done

