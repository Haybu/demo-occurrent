reference: https://gist.github.com/asoorm/7822cc742831639c93affd734e97ce4f

I followed the steps below to run a simple replica set with one member for local development, :

1- run docker-compose up -d with the below docker-compose file

version: "3.4"
services:
  mongo1:
    hostname: mongodb
    container_name: mongodb
    image: mongo:latest
    environment:
        MONGO_INITDB_DATABASE: {DATABASE NAME}
        MONGO_REPLICA_SET_NAME: rs0
    expose:
      - 27017
    ports:
      - 27017:27017
    restart: always
    entrypoint: [ "/usr/bin/mongod", "--bind_ip_all", "--replSet", "rs0" ]

2- shell into the container: docker exec -it mongodb /bin/bash

3- execute mongo command in the container's shell

4- register the replica set member by running

rs.initiate({
      _id: "rs0",
      version: 1,
      members: [
         { _id: 0, host : "localhost:27017" }
      ]
   }
)

5- use this URL to connect: mongodb://localhost:27017/{DATABASE NAME}?replicaSet=rs0