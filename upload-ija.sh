#!/bin/sh

set -e
if [ -z "$1" ]; then
	VERSION="$(git log -1 --pretty=format:%s HEAD^2 |
                sed -n "s/^[^0-9]*\([\\.0-9]*.\),.*$/\1/p")"
	test -z "$VERSION" && {
		echo "Could not determine version"
		exit 1
	}
else
	VERSION="$VERSION"
fi

# CVS
CVS=.git/cvs-checkout
test -d $CVS ||
(cd .git &&
 cvs -d :ext:imageja.cvs.sf.net:/cvsroot/imageja co imageja &&
 mv imageja cvs-checkout || {
	echo "Could not checkout CVS"
	exit 1
 }
)
(cd $CVS &&
 git-cvsimport -a -i -k -p -u,-b,HEAD) || {
	echo "Could not update CVS"
	exit 1
}

test -f .git/objects/info/alternates ||
echo "$(pwd)/$CVS/.git/objects" > .git/objects/info/alternates

test -z "$(git config remote.cvs.url)" && {
	git config remote.cvs.url "$(pwd)/$CVS" &&
	git config remote.cvs.fetch origin:refs/heads/cvs
}

git fetch cvs || {
	echo "Could not fetch 'origin' from CVS checkout"
	exit 1
}

test -z "$(git config alias.cvscione)" &&
git config alias.cvscione \
	"!sh -c 'git cvsexportcommit -w $CVS -c -p -u \$0^ \$0'"

test "$(git log -1 --pretty=format:%s%n%b cvs)" != \
		"$(git log -1 --pretty=format:%s%n%b imageja^)" || {
	git cvscione imageja &&
	(cd $CVS && git-cvsimport -a -i -k -p -u,-b,HEAD) &&
	git fetch cvs || {
		echo "Could not update CVS"
		exit 1
	}
}

test "$(git log -1 --pretty=format:%s%n%b cvs)" = \
		"$(git log -1 --pretty=format:%s%n%b imageja)" || {
	echo "CVS lags behind..."
	exit 1
}

git tag v"$VERSION" imageja
git push dumbo
git push orcz imageja:master imageja imagej v"$VERSION"
make signed-ij.jar
rsync -vau signed-ij.jar shell.sf.net:imageja/htdocs/ij.jar
git archive --format=zip --prefix=ij-src/ imageja > ij-src-$VERSION.jar
mv ij.jar ij-$VERSION.jar
rsync -e ssh -avP ij-$VERSION.jar ij-src-$VERSION.jar frs.sf.net:uploads/
echo w3m http://sourceforge.net/project/admin/editpackages.php?group_id=150609

