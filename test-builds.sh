#!/bin/bash

function readLink () {
	x=$(ls -l "$@" 2>/dev/null | sed "s/^.*-> //")
	if [ -z "$x" ]; then
		echo "$1"
	else
		echo "$x"
	fi
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

