# This script compiles imagej into imagej.js
# It requires CHEERPJ_DIR and IJ_DIR

set -e

mvn install:install-file -Dfile=${CHEERPJ_DIR}/cheerpj-dom.jar -DgroupId=com.learningtech -DartifactId=cheerpj-dom -Dversion=1.0 -Dpackaging=jar
mvn -Pdeps package
cp target/ij-1.53c.jar ${IJ_DIR}/ij.jar

cd ${IJ_DIR}
${CHEERPJ_DIR}/cheerpjfy.py ij.jar
# ${CHEERPJ_DIR}/cheerpjfy.py  --deps=ij.jar plugins/Thunder_STORM.jar