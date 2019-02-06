#!/usr/bin/env bash

# close all connections - this means shutting down the three tomcat instances
# if you don't first you'll see a message like:
# ERROR:  database "netprof" is being accessed by other users
# DETAIL:  There are 60 other sessions using the database.

date
curl -udmendoza -o /tmp/netprof_pg.dump "https://kws-bugs.ll.mit.edu/artifactory/dli-archiving/ltea-data2-pg/daily/netprof_pg.dump"
date

# have to be postgres first
#sudo su - postgres

# assumes postgres is owner of old netprof_old
# assumes we've done this before
# so if everything goes really badly we can go back, and lets us create a new netprof database

date
psql -U postgres --host 127.0.0.1 -c "drop database netprof_old;"
psql -U postgres --host 127.0.0.1 -c "alter database netprof rename to netprof_old;"

# create database from dump
date
pg_restore -d netprof -U netprof -C /tmp/netprof_pg.dump

date
echo "Done!"
