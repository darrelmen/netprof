#!/usr/bin/env bash

for langlc in `ls /opt/netprof/bestAudio | grep -E 'korean|levantine|msa|russian'`;
do
    #lang=${file:12}
    #langlc="${lang,,}"
    echo $langlc

    suffix="BestAudio.tar.gz"
    toPost=$langlc$suffix
    echo "Posting best audio for $toPost"

    echo `pwd`

    mkdir -p /tmp/tempBestAudio/$langlc
    cd /opt/netprof/bestAudio/$langlc

    echo `pwd`

    date
    echo "before tar"

    tar cfz /tmp/tempBestAudio/$langlc/$toPost --exclude='*.raw, *_16K.wav, *_16K_*.wav' bestAudio

    date
    echo "after  tar"

    cd /tmp/tempBestAudio/$langlc/

    for file2 in `ls`
    do
        echo `pwd`
        echo $file2
        ls -l $file2

        date
        echo "before curl"

        echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra/$langlc/bestAudio/$file2"
        curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra/$langlc/bestAudio/$file2"

        date
        echo "after  curl"

        echo "remove $file2"
        rm $file2
    done

done


