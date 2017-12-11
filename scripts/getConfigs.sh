#!/usr/bin/env bash

# go get all the netprof languages from artifactory
# fill in the curl user below with your user id

cd /opt/netprof/import
echo $pwd

files=(cm croatianeval dari egyptian english farsi french german hindieval iraqi japanese korean levantine msa pashto1 pashto2 pashto3 portuguese russian serbian soranieval spanish sudanese tagalog turkisheval urdu)
for file in ${files[@]}
do
echo $file
cd config
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-materials/NetProF-Audio/$file/$file.tar.gz"
tar xvfz $file.tar.gz
rm $file.tar.gz
cd ..

done