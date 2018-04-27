#!/usr/bin/env bash

# run this on ltea-data2

date

echo "start dumping postgres now..."
# this dumps out a special postgres compressed dump
PGPASSWORD=npadmin pg_dump -U netprof --host 127.0.0.1 -f netprof_pg.dump netprof -Fc
echo "finished dumping postgres now..."

date
echo "start posting to artifactory now..."
curl -ugvidaver -T netprof_pg.dump  "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/ltea-data2-pg/hourly/netprof_pg.dump"
date
echo "Done!"
