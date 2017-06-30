#!/usr/bin/env bash

for file in `ls -l /etc/init | grep dcodr | grep -v "~" | awk '{print $9}' | sed 's/.conf//'`; do date >> start.log ; echo $file >> start.log; sudo -u root start $file >> start.log; sleep 1; done
