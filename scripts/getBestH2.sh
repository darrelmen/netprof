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

path=/opt/netprof/bestAudio/$file
mkdir $path
cd $path

echo `pwd`

echo "start curl..."
date
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$orig/bestAudio/$orig.tar.gz"
echo "finished curl..."

date
echo "start untar..."
tar xfz $orig.tar.gz
echo "done untar..."
rm $orig.tar.gz
date
}

files=(korean levantine msa russian)
for file in ${files[@]}
do
 fun $file
done

sudo chown -R tomcat8 /opt/netprof/bestAudio/