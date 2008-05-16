#!/bin/sh

set -e
if [ -z "$1" ]; then
	VERSION=$(git tag -l "ij*" | sort | tail -n 1 | sed "s/^ij\(.\)/\1./")
else
	VERSION="$VERSION"
fi
(cd ../cvs-imageja && my-fetch-origin.sh)
git tag v"$VERSION" origin
git push dumbo
git push orcz imageja:master imageja v"$VERSION"
make signed-ij.jar
rsync -vau signed-ij.jar dscho@shell.sf.net:imageja/htdocs/ij.jar
git archive --format=zip --prefix=ij-src/ origin > ij-src-$VERSION.jar
mv ij.jar ij-$VERSION.jar
rsync -e ssh -avP ij-$VERSION.jar ij-src-$VERSION.jar dscho@frs.sf.net:uploads/
echo w3m http://sourceforge.net/project/admin/editpackages.php?group_id=150609

