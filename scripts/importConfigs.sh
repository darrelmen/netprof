#!/usr/bin/env bash

# it's OK if we try to do it again - it's smart enough to check if it's already there                                                                                                                                                                                                                                             
config=(cm croatianeval dari egyptian english farsi french german hindieval iraqi japanese korean levantine msa portuguese russian serbian soranieval spanish sudanese tagalog turkisheval urdu)
for file in ${config[@]}
do
    date
    echo $file
    if ./copy.sh $file > $file.log; then
     echo copied #file
    else
     echo "ERROR : couldn't copy $file"
     break
    fi
done

echo pashto1
./copy.sh pashto1 -n Elementary > pashto1.log

echo pashto2
./copy.sh pashto2 -o 1 Intermediate > pashto2.log

echo pashto3
./copy.sh pashto3 -o 2 Advanced > pashto3.log

date
