#!/usr/bin/env bash

#sites=(npfClassroomCroatianEval)
#$for file in ${sites[@]}
for file in `ls ~/webapps | grep npfClass | grep -v war| grep -v Test | grep -v FrenchEval | grep -v SudaneseEval`;
do
lang=${file:12}
langlc="${lang,,}"
echo $langlc
suffix="Answers.tar.gz"
toPost=$langlc$suffix
echo $toPost

mkdir -p /data/tempAnswers/$langlc
cd ~/webapps/$file

tar cfz /data/tempAnswers/$langlc/$toPost answers
cd /data/tempAnswers/$langlc/
#split -b5G $toPost $langlc

for file2 in `ls`
do
echo $file2
echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/answersAudio/$file2"
done

rm /data/tempAnswers/$langlc/*
#break;

done

