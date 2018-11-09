#!/usr/bin/env bash

for langlc in `ls /opt/netprof/answers | grep -v korean | grep -v levantine | grep -v msa | grep -v russian`;
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

    tar cfz /tmp/tempAnswers/$langlc/$toPost --exclude='*.raw, *_16K.wav, *_16K_*.wav' answers

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


