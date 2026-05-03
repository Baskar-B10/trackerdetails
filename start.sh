#!/bin/bash
mkdir -p /home/runner/data/db
mongod --dbpath /home/runner/data/db --port 27017 --bind_ip 127.0.0.1 --fork --logpath /home/runner/data/mongod.log
echo "MongoDB started"
mvn spring-boot:run
