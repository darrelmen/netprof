#!/usr/bin/env bash

for file in `ls -l /etc/init | grep dcodr | grep -v "~" | awk '{print $9}' | sed 's/.conf//'`; do date >> stop.log ; echo $file >> stop.log; sudo -u root stop $file >> stop.log; sleep 1; done
