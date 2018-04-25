#!/usr/bin/env bash

date
export PGPASSWORD=npadmin
echo $PGPASSWORD
pg_dump -U netprof netprof --host 127.0.0.1 > ltea_data2.dump
date
ls -l ltea_data2.dump
date
tar cfz ltea_data2_dump.tar.gz ltea_data2.dump
date
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -T ./ltea_data2_dump.tar.gz "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/ltea-data2-pg/daily/ltea_data2_dump.tar.gz"
date
echo "Done!"
