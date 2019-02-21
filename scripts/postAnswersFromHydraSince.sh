#!/usr/bin/env bash

for langlc in `ls /opt/netprof/answers | grep -v korean | grep -v levantine | grep -v msa | grep -v russian`;
do
    echo $langlc

    suffix="AnswersIncremental.tar.gz"
    toPost=$langlc$suffix
    echo "Posting answers for $toPost"

    echo `pwd`

    mkdir -p /tmp/tempAnswers/$langlc
    cd /opt/netprof/answers/$langlc

    echo `pwd`

    date
    echo "before tar"

#    tar cfz /tmp/tempAnswers/$langlc/$toPost --exclude='*.raw, *_16K.wav, *_16K_*.wav' answers
    tar -cfz /tmp/tempAnswers/$langlc/$toPost --newer-mtime='2018-08-01 23:59:59'  --exclude='*.raw' --exclude='*_16K.wav' --exclude='*_16K_*.wav' --exclude='orig_*.wav' --exclude='trim_*.wav' --exclude='*.lab' tempAnswers

    date
    echo "after  tar"

    cd /tmp/tempAnswers/$langlc/

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

        echo "remove $file2"
        rm $file2
    done

done


