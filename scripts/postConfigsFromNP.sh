#!/usr/bin/env bash

for file in `ls ~/webapps | grep npfClass | grep -v war| grep -v Test | grep -v FrenchEval | grep -v SudaneseEval`;
do
lang=${file:12}
langlc="${lang,,}"
echo $langlc

mkdir /data/temp/$langlc
cp ~/webapps/$file/config/*/quizlet.properties /data/temp/$langlc
cp ~/webapps/$file/config/*/*.xlsx /data/temp/$langlc
cp ~/webapps/$file/config/*/*.h2.db /data/temp/$langlc
cd /data/temp
tar cvfz /data/temp/$langlc.tar.gz $langlc
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T /data/temp/$langlc.tar.gz "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/$langlc.tar.gz"

done