#!/bin/bash

exec /opt/dcodr/scala_2.11.2/bin/scala -Dfile.encoding=UTF-8 -classpath "/opt/dcodr/jars/npws-assembly-0.2-SNAPSHOT.jar" dcodr.Driver /opt/dcodr/spanish/webservice-properties.txt /opt/dcodr/bin.linux64/wsdcodr localhost 31196 &>> /opt/dcodr/logs/spanish.log