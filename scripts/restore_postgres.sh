#!/usr/bin/env bash

date
#export PGPASSWORD=npadmin
#echo $PGPASSWORD
#PGPASSWORD=npadmin pg_dump -U netprof netprof --host 127.0.0.1 > ltea_data2.dump
curl -ugvidaver -o /tmp/netprof_pg.dump "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/ltea-data2-pg/daily/netprof_pg.dump"
date
#tar xvfz ltea_data2_dump.tar.gz
#date
# for now let's try to go to a sample database
#PGPASSWORD=npadmin psql -U netprof netprof_prod --host 127.0.0.1 < ltea_data2.dump
#date

#PGPASSWORD=npadmin psql -U netprof netprof --host 127.0.0.1 -c "drop database netprof_old;"
#PGPASSWORD=npadmin psql -U netprof netprof --host 127.0.0.1 < "alter database netprof rename to netprof_old;"
#PGPASSWORD=npadmin pg_restore -d netprof -U netprof -C netprof_pg.dump


# have to be postgres first
#sudo su - postgres

# assumes postgres is owner of old netprof_old
# assumes we've done this before
date
psql -U postgres --host 127.0.0.1 -c "drop database netprof_old;"

# so if everything goes really badly we can go back, and lets us create a new netprof database

# close all connections - this means shutting down the three tomcat instances
# if you don't first you'll see a message like:
# ERROR:  database "netprof" is being accessed by other users
# DETAIL:  There are 60 other sessions using the database.

psql -U postgres --host 127.0.0.1 -c "alter database netprof rename to netprof_old;"

# create database from dump
date
pg_restore -d netprof -U netprof -C /tmp/netprof_pg.dump

date
echo "Done!"
