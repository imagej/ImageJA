#!/bin/bash

for i in .git/refs/heads/*; do
	name=$(basename $i)
	if [ $name != cumul -a $name != master -a $name != tools ]; then
		git checkout -f $name || exit 1
		git-resolve $name master master || exit 3
	fi
done

