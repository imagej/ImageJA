#!/bin/bash

#
# This script has benn derived from travis-build.sh, a script to build and/or release SciJava-based projects.
#

dir="$(dirname "$0")"

success=0
checkSuccess() {
	# Log non-zero exit code.
	test $1 -eq 0 || echo "==> FAILED: EXIT CODE $1" 1>&2

	# Record the first non-zero exit code.
	test $success -eq 0 && success=$1
}

# Build Maven projects.
if [ -f pom.xml ]
then
	echo "= Maven build ="
	echo
	echo "== Configuring Maven =="

	# NB: Suppress "Downloading/Downloaded" messages.
	# See: https://stackoverflow.com/a/35653426/1207769
	export MAVEN_OPTS="$MAVEN_OPTS -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn" 

	# Populate the settings.xml configuration.
	mkdir -p "$HOME/.m2"
	settingsFile="$HOME/.m2/settings.xml"
	customSettings=.travis/settings.xml
	if [ -f "$customSettings" ]
	then
		cp "$customSettings" "$settingsFile"
	else
		cat >"$settingsFile" <<EOL
<settings>
	<servers>
		<server>
			<id>sonatype-nexus-releases</id>
			<username>scijava-ci</username>
			<password>\${env.OSSRH_PASS}</password>
		</server>
	</servers>
EOL
		# NB: Use maven.scijava.org as sole mirror if defined in <repositories>.
		# This hopefully avoids intermittent "ReasonPhrase:Forbidden" errors
		# when the Travis build pings Maven Central; see travis-ci/travis-ci#6593.
		grep -A 2 '<repository>' pom.xml | grep -q 'maven.scijava.org' &&
		cat >>"$settingsFile" <<EOL
	<mirrors>
		<mirror>
			<id>scijava-mirror</id>
			<name>SciJava mirror</name>
			<url>https://maven.scijava.org/content/groups/public/</url>
			<mirrorOf>*</mirrorOf>
		</mirror>
	</mirrors>
EOL
		cat >>"$settingsFile" <<EOL
	<profiles>
		<profile>
			<id>gpg</id>
			<activation>
				<file>
					<exists>\${env.HOME}/.gnupg</exists>
				</file>
			</activation>
			<properties>
				<gpg.keyname>\${env.GPG_KEY_NAME}</gpg.keyname>
				<gpg.passphrase>\${env.GPG_PASSPHRASE}</gpg.passphrase>
			</properties>
		</profile>
	</profiles>
</settings>
EOL
	fi

	# Install GPG on OSX/macOS
	if [ "$TRAVIS_OS_NAME" = osx ]
	then
		HOMEBREW_NO_AUTO_UPDATE=1 brew install gnupg2
	fi

	# Import the GPG signing key.
	keyFile=.travis/signingkey.asc
	key=$1
	iv=$2
	if [ "$key" -a "$iv" -a -f "$keyFile.enc" ]
	then
		# NB: Key and iv values were given as arguments.
		echo
		echo "== Decrypting GPG keypair =="
		openssl aes-256-cbc -K "$key" -iv "$iv" -in "$keyFile.enc" -out "$keyFile" -d
		checkSuccess $?
	fi
	if [ "$TRAVIS_SECURE_ENV_VARS" = true \
		-a "$TRAVIS_BRANCH" = "master" \
		-a -f "$keyFile" ]
	then
		echo
		echo "== Importing GPG keypair =="
		gpg --batch --fast-import "$keyFile"
		checkSuccess $?
	fi

	# Run the build.
	if [ "$TRAVIS_SECURE_ENV_VARS" = true \
		-a "$TRAVIS_BRANCH" = "master" ]
	then
#		version=`mvn org.apache.maven.plugins:maven-help-plugin::evaluate -Dexpression=project.version | grep -v "^\["`
#		echo "== Cutting and deploying release version $version=="
#		version=v`echo $version | cut -d'-' -f 1`
#    mvn release:prepare-with-pom -DconnectionUrl=scm:svn:https://github.com/imagej/ImageJA/tree/$version
#		mvn -B release:perform -DconnectionUrl=scm:git:https://github.com/imagej/ImageJA
    NEXUS_URL=https://maven.scijava.org/ 
    SONATYPE_PROXY=$NEXUS_URL/service/local/data_cache/repositories/sonatype/content

    git clean -fdx

    mvn clean &&
    mvn -Psonatype-oss-release deploy &&
    curl --netrc -i -X DELETE \
       $SONATYPE_PROXY/net/imagej/ij/maven-metadata.xml
		checkSuccess $?
	else
		echo
		echo "== Building the artifact locally only =="
		mvn -B install
		checkSuccess $?
	fi
fi

exit $success
