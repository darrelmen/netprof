#!/usr/bin/env bash

# it's OK if we try to do it again - it's smart enough to check if it's already there
config=("dari" "egyptian" "english" "farsi" "french" "german" "iraqi" "japanese" "korean" "levantine" "mandarin" "msa" "portuguese" "russian" "spanish" "sudanese" "tagalog" "urdu" "croatian" "hindi" "sorani")
for file in ${config[@]}
do
    echo $file
    ./copy.sh $file > $file.log
done

echo pashto1
./copy.sh pashto1 pashtoQuizlet1.properties 0 Pasho Elementary

echo pashto2
./copy.sh pashto2 pashtoQuizlet2.properties 1 Pasho Intermediate

echo pashto3
./copy.sh pashto3 pashtoQuizlet3.properties 2 Pasho Advanced
