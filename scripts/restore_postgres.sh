#!/usr/bin/env bash

date
export PGPASSWORD=npadmin
echo $PGPASSWORD
#PGPASSWORD=npadmin pg_dump -U netprof netprof --host 127.0.0.1 > ltea_data2.dump
curl -ugvidaver:AP7UBZfNhCphhouwNrWyL2WqWX -O "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/ltea-data2-pg/daily/ltea_data2_dump.tar.gz"
date
tar xvfz ltea_data2_dump.tar.gz
date
# for now let's try to go to a sample database
psql -U netprof netprof_prod --host 127.0.0.1 < ltea_data2.dump
date
echo "Done!"

