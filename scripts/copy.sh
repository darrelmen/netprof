#!/usr/bin/env bash
export certs="-Djavax.net.ssl.keyStore=/etc/ssl/local/mongodb.jks -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.keyStorePassword=changeit123 -Djavax.net.ssl.trustStore=/etc/ssl/local/cacerts -Djavax.net.ssl.trustStorePassword=changeit123"
java $certs -cp "/opt/netprof/installation/current/lib/*" mitll.langtest.server.database.copy.CopyToPostgres copy $1 > $1.log