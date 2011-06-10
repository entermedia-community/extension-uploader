rm -rf keystore
mkdir keystore
keytool -genkey -alias openedit-upload -validity 2100 -keystore keystore/openedit < keystore.conf
