#!/usr/bin/env bash
export certs="-Djavax.net.ssl.keyStore=/etc/ssl/local/mongodb.jks -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.keyStorePassword=63Roland3xX -Djavax.net.ssl.trustStore=/etc/ssl/local/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
echo "make sure these certs are correct $certs"
echo "if the certs are not correct, you won't be able to connect to mongo"
java $certs -cp "/opt/netprof/installation/current/lib/*" mitll.langtest.server.database.copy.CopyToPostgres -c $@
