#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

# remove existing keys
rm -f com.gsma.iariauth0.sample*

# create entity key for specific range iari.
echo "create entity cert for specific range iari"
keytool -genkey -keyalg RSA -alias com.gsma.iariauth0.sample -keystore com.gsma.iariauth0.sample.jks -storepass secret -keypass secret -dname CN=iari.range.test -keysize 2048
#keytool -list -keystore com.gsma.iariauth0.sample.jks -storepass secret -keypass secret -v

# create csr for entity cert.
echo "create csr for entity cert"
keytool -certreq -keyalg RSA -alias com.gsma.iariauth0.sample -keystore com.gsma.iariauth0.sample.jks -storepass secret -keypass secret -dname CN=iari.range.test -file com.gsma.iariauth0.sample.csr

# sign entity cert using range cert
echo "sign entity cert using range cert"
openssl x509 -req -CA range-root.pem -CAkey range-root.pem -in com.gsma.iariauth0.sample.csr -out com.gsma.iariauth0.sample.cert -days 365 -CAcreateserial -extfile _iarilist-range.ext
#openssl x509 -req -CA range-root.pem -CAkey range-root.pem -in com.gsma.iariauth0.sample.csr -out com.gsma.iariauth0.sample.cert -days 365 -CAcreateserial -extfile _iarilist-stdalone.ext

# import root cert into keystore
echo "import root cert into keystore"
keytool -importcert -keystore com.gsma.iariauth0.sample.jks -file range-root.cert -alias range-root -noprompt -storepass secret -keypass secret

# import entity cert into keystore
echo "import entity cert into keystore"
keytool -importcert -keystore com.gsma.iariauth0.sample.jks -file com.gsma.iariauth0.sample.cert -alias com.gsma.iariauth0.sample -storepass secret -keypass secret
