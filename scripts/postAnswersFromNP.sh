#!/usr/bin/env bash

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

date
echo "before tar"

tar cfz /data/tempAnswers/$langlc/$toPost --exclude='*.raw' answers

date
echo "after  tar"

cd /data/tempAnswers/$langlc/
#split -b5G $toPost $langlc

for file2 in `ls`
do

echo `pwd`
echo $file2
ls -l $file2

date
echo "before curl"

echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/answersAudio/$file2"

curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/answersAudio/$file2"

date
echo "after  curl"

done

date
echo "before rm"

rm /data/tempAnswers/$langlc/*

date
echo "after  rm"

done

