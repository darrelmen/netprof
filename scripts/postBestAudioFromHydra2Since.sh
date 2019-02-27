#!/usr/bin/env bash

for langlc in `ls /opt/netprof/bestAudio | grep -E 'korean|levantine|msa|russian'`;
do
    echo $langlc

    suffix="BestAudioIncremental.tar.gz"
    toPost=$langlc$suffix
    echo "Posting best audio for $toPost"

    echo `pwd`

    mkdir -p /tmp/tempBestAudio/$langlc
    cd /opt/netprof/bestAudio/

    echo `pwd`

    date
  #  echo "before tar"

 # newer than aug 1 2018
    echo tar cfz /tmp/tempBestAudio/$langlc/$toPost --newer-mtime='2018-08-01 23:59:59'  --exclude='*.raw' --exclude='*_16K.wav' --exclude='*_16K_*.wav' --exclude='orig_*.wav' --exclude='trim_*.wav' --exclude='*.lab' $langlc
    tar cfz /tmp/tempBestAudio/$langlc/$toPost --newer-mtime='2018-08-01 23:59:59'  --exclude='*.raw' --exclude='*_16K.wav' --exclude='*_16K_*.wav' --exclude='orig_*.wav' --exclude='trim_*.wav' --exclude='*.lab' $langlc

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

        echo curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra2/$langlc/bestAudio/$file2"
        curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T $file2 "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/NetProF-Audio-Hydra2/$langlc/bestAudio/$file2"

        date
        echo "after  curl"

        echo "remove $file2"
        rm $file2
    done

done


