make signed-ij.jar
scp signed-ij.jar dscho@shell.sf.net:imageja/htdocs/ij.jar
git archive --format=tar --prefix=ij-src origin | gzip -9 > ij-src.tar.gz
curl -T "{ij.jar,ij-src.tar.gz}" ftp://upload.sourceforge.net/incoming/
echo w3m http://sourceforge.net/project/admin/editreleases.php
