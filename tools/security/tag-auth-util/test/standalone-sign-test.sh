#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

# echo remove old keys etc
rm -f iari-standalone-authorization.xml
rm -f *standalone*.xml
rm -f keys/*standalone*

# generate new standalone iari
echo "create standalone iari"
java -jar ../build/iaritool.jar -generate -keyalg RSA -alias iari-standalone-test -keystore keys/iari-standalone-test.jks -storepass secret -keypass secret -dname CN=iari.standalone.test -validity 360 -keysize 2048 -dest iari-standalone-authorization.xml -v

# sign package with that iari
echo "create iari authorization for package"
java -jar ../build/iaritool.jar -sign -template iari-standalone-authorization.xml -dest iari-standalone-authorization.xml -alias iari-standalone-test -keystore keys/iari-standalone-test.jks -storepass secret -keypass secret -pkgname com.gsma.iariauth.sample -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v

# validate auth document
echo "validate signed iari authorization"
java -jar ../../tag-auth-validator/build/iarivalidator.jar -d iari-standalone-authorization.xml -pkgname com.gsma.iariauth.sample -keystore keys/range-root-truststore.jks -storepass secret -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v
