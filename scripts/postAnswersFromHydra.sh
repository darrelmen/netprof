#!/usr/bin/env bash

for langlc in `ls /opt/netprof/answers`;
do
#lang=${file:12}
#langlc="${lang,,}"
echo $langlc

suffix="Answers.tar.gz"
toPost=$langlc$suffix
echo "Posting answers for $toPost"

echo `pwd`

mkdir -p /tmp/tempAnswers/$langlc
cd /opt/netprof/answers/$langlc

echo `pwd`

date
echo "before tar"

tar cfz /tmp/tempAnswers/$langlc/$toPost --exclude='*.raw' answers

date
echo "after  tar"

cd /tmp/tempAnswers/$langlc/
#split -b5G $toPost $langlc

for file2 in `ls`
do

echo `pwd`
echo $file2
ls -l $file2

date
echo "before curl"

#echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/answersAudio/$file2"
echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra/$langlc/answersAudio/$file2"

#curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$langlc/answersAudio/$file2"
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra/$langlc/answersAudio/$file2"

date
echo "after  curl"

done

date
echo "before rm"

#rm /tmp/tempAnswers/$langlc/*

date
echo "after  rm"

#break;

done


