#!/usr/bin/env bash

for file in `ls -l /etc/init | grep dcodr | grep -v "~" | awk '{print $9}' | sed 's/.conf//'`; do date >> restart.log ; echo $file >> restart.log; sudo -u root restart $file >> restart.log; sleep 1; done
