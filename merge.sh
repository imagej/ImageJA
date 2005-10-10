#!/bin/bash

git checkout -f cumul || exit 1

for i in .git/refs/heads/*; do
	name=$(basename $i)
	if [ $name != cumul ]; then
		echo "Trying to merge $name"
		git-resolve HEAD $name $name || exit 3
	fi
done

