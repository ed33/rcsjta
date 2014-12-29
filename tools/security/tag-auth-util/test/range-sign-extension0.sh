#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

# echo remove old keys etc
rm -f extension0.xml

# generate new range iari template
echo "create range iari template"
java -jar ../build/iaritool.jar -init -iari urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.extension0 -range urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.* -dest extension0.xml -v

# sign package with that iari
echo "create iari authorization for package"
java -jar ../build/iaritool.jar -sign -template extension0.xml -dest extension0.xml -alias com.gsma.iariauth0.sample -keystore keys/com.gsma.iariauth0.sample.jks -storepass secret -keypass secret -pkgname com.gsma.iariauth0.sample -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v

# validate auth document
echo "validate signed iari authorization"
java -jar ../../tag-auth-validator/build/iarivalidator.jar -d extension0.xml -pkgname com.gsma.iariauth0.sample -keystore keys/range-root-truststore.jks -storepass secret -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass secret -v
