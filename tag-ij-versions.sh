#!/bin/sh

cd "$(dirname "$0")/.." &&
git log --format='%H %s' origin/imagej |
sed -n 's/^\([0-9a-f]\{40\}\)   . \(1.[1-9][0-9]*.\?\), [0-9][1-9]* .*/\1 \2/p' |
while read commit version
do
	test $commit = 9ae14740ea3ea6311a273c9eab55aed6b54e33ef &&
	continue # Wayne released 1.42q twice, we take the younger one
	sha1="$(git rev-parse refs/tags/ij-$version^{commit} 2> /dev/null)" || {
		git tag -a -m "ImageJ $version" ij-$version $commit
		continue
	}
	test "$commit" = "$sha1" ||
	echo "Error: tag ij-$version points to $sha1, not $commit"
done
