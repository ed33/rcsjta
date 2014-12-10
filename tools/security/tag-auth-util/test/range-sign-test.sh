#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

# echo remove old keys etc
rm -f iari-range-authorization.xml

# generate new range iari template
echo "create range iari template"
java -jar ../build/iaritool.jar -init -iari urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.iari-range-test -range urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.* -dest iari-range-authorization.xml -v

# sign package with that iari
echo "create iari authorization for package"
java -jar ../build/iaritool.jar -sign -template iari-range-authorization.xml -dest iari-range-authorization.xml -alias com.gsma.iariauth.sample -keystore keys/com.gsma.iariauth.sample.jks -storepass secret -keypass secret -pkgname com.gsma.iariauth.sample -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v

# validate auth document
echo "validate signed iari authorization"
java -jar ../../tag-auth-validator/build/iarivalidator.jar -d iari-range-authorization.xml -pkgname com.gsma.iariauth.sample -keystore keys/range-root-truststore.jks -storepass secret -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v
