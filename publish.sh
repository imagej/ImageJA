#!/bin/bash

if [ -z "$1" ]; then
	echo "Need a path"
	exit 1
fi

destdir="$1"

version=$(git-cat-file commit master | \
	sed -n -e "s/^v\([1-9]\)/Version \1/" \
		-e "s/^  \* v\([1-9]\)/Version \1/" \
		-e "/^Version/s/^.*1\.\([0-9][^, ]*\).*$/1\1/p" | \
	head -n 1)

for i in .git/refs/heads/*; do
	name=$(basename $i)
	if [ $name != master -a $name != tools -a $name != imageja ]; then
		git diff master..${name} > "$destdir"/ij$version-$name.patch || exit 1
		git-tar-tree $name | gzip -9 > "$destdir"/ij$version-$name.tar.gz || exit 2
	fi
done

