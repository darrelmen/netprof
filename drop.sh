#!/usr/bin/env bash
java -Djavax.net.ssl.keyStore=/etc/ssl/local/mongodb.jks -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.keyStorePassword=changeit123 -Djavax.net.ssl.trustStore=/etc/ssl/local/cacerts -Djavax.net.ssl.trustStorePassword=changeit123 -cp WEB-INF/lib/*:WEB-INF/classes mitll.langtest.server.database.copy.CopyToPostgres drop $1
