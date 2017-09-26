#!/usr/bin/env bash

# go get all the netprof languages from artifactory
# fill in the curl user below with your user id

fun() {
echo $1
local file=$1
local orig=$1
echo $orig
if [ $file = "cm" ]
then
 file="mandarin"
elif [ $file = "croatianeval" ]
then
 file="croatian"
elif [ $file = "hindieval" ]
then
 file="hindi"
elif [ $file = "pashto1" ]
then
 file="pashto"
elif [ $file = "pashto2" ]
then
 file="pashto"
elif [ $file = "pashto3" ]
then
 file="pashto"
elif [ $file = "soranieval" ]
then
 file="sorani"
elif [ $file = "turkisheval" ]
then
 file="turkish"
fi

cd /opt/netprof/answers/$file

echo `pwd`

date
suffix="Answers"
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$orig/answersAudio/$orig$suffix.tar.gz"

date
tar xfz $orig.tar.gz
rm $orig.tar.gz
#sudo chown -R tomcat8 .
date

cd ..
}

files=( korean levantine msa russian)
for file in ${files[@]}
do
 fun $file
done
