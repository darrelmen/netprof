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

path=/opt/netprof/answers/$file
cd $path

echo `pwd`

echo "start curl..."
date
suffix="Answers"
realName=$orig$suffix
echo $realName

echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$orig/answersAudio/$realName.tar.gz"
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$orig/answersAudio/$realName.tar.gz"

echo "finished curl..."
date
tar xfz $realName.tar.gz
echo "done untar..."
rm $realName.tar.gz
date



cd ..
}

files=(cm croatianeval dari egyptian english farsi french german hindieval iraqi japanese korean levantine msa pashto1 pashto2 pashto3 portuguese russian serbian soranieval spanish sudanese tagalog turkisheval urdu)
for file in ${files[@]}
do
 fun $file
done
