set -x
if [ -z "$1" ]; then
	echo "need a version"
	exit 1
fi
my-fetch-origin.sh
git tag v"$1" origin
git push vib
git push dumbo
git push orcz
make signed-ij.jar
scp signed-ij.jar dscho@shell.sf.net:imageja/htdocs/ij.jar
git archive --format=zip --prefix=ij-src/ origin > ij-src-$1.jar
mv ij.jar ij-$1.jar
curl -T "{ij-$1.jar,ij-src-$1.jar}" ftp://upload.sourceforge.net/incoming/
echo w3m http://sourceforge.net/project/admin/editpackages.php?group_id=150609

