#!/bin/bash

for i in .git/refs/heads/*; do
	name=$(basename $i)
	if [ $name != cumul -a $name != master ]; then
		git checkout -f $name || exit 1
		git-resolve-script $name master master || exit 3
	fi
done

