#!/bin/bash

# do not use this one, rather use a replica set by running:
# $ docker-compose up -d

docker run --rm -d -p 27017:27017 --name some-mongo \
    -e MONGO_INITDB_ROOT_USERNAME=mongoadmin \
    -e MONGO_INITDB_USERNAME=mongoadmin \
    -e MONGO_INITDB_ROOT_PASSWORD=secret \
    -e MONGO_INITDB_PASSWORD=secret \
    -e MONGO_INITDB_DATABASE=database \
    mongo