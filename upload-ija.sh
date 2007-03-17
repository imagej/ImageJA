make signed-ij.jar
scp signed-ij.jar dscho@shell.sf.net:imageja/htdocs/ij.jar
git archive --format=zip --prefix=ij-src/ origin > ij-src.jar
curl -T "{ij.jar,ij-src.jar}" ftp://upload.sourceforge.net/incoming/
echo w3m http://sourceforge.net/project/admin/editpackages.php?group_id=150609

