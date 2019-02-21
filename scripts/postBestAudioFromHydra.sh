#!/usr/bin/env bash

for langlc in `ls /opt/netprof/bestAudio | grep -v korean | grep -v levantine | grep -v msa | grep -v russian`;
do
    #lang=${file:12}
    #langlc="${lang,,}"
    echo $langlc

    suffix="BestAudio.tar.gz"
    toPost=$langlc$suffix
    echo "Posting best audio for $toPost"

    echo `pwd`

    mkdir -p /tmp/tempBestAudio/$langlc
    cd /opt/netprof/bestAudio

    echo `pwd`

    date
    echo "before tar"

    echo tar cfz /tmp/tempBestAudio/$langlc/$toPost -N date "01 Aug 2018" --exclude='*.raw, *_16K.wav, *_16K_*.wav, orig_*.wav, trim_*.wav, *.lab, *.ogg' $langlc
    tar cfz /tmp/tempBestAudio/$langlc/$toPost -N date "01 Aug 2018" --exclude='*.raw, *_16K.wav, *_16K_*.wav, orig_*.wav, trim_*.wav, *.lab, *.ogg' $langlc

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


