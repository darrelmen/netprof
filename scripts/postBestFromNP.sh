#!/usr/bin/env bash

#sites=(npfClassroomCroatianEval)
#$for file in ${sites[@]}

date

for file in `ls ~/webapps | grep npfClass | grep -v war| grep -v Test | grep -v FrenchEval | grep -v SudaneseEval`;
do
lang=${file:12}
langlc="${lang,,}"
echo $langlc
toPost=$langlc.tar.gz
echo $toPost

mkdir -p /data/tempBest/$langlc
cd ~/webapps/$file

tar cfz /data/tempBest/$langlc/$toPost bestAudio
cd /data/tempBest/$langlc/
#split -b5G $toPost $langlc

for file2 in `ls`
do
date
curl -ugvidaver -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/bestAudio/$file2"
date
done

rm /data/tempBest/$langlc/*
date
done