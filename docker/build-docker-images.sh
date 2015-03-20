#!/bin/bash

POSTGRES=$(docker images | grep postgres | awk '{print $1}')
if [[ -z $POSTGRES ]]; then
  echo "Pulling postgres image from public registry"
  docker pull postgres
fi

echo "Builing Minion image"
docker build -t minion:v1 ./minion

echo "Builing Dominion image"
docker build -t dominion:v1 ./dominion

echo "Building snmpd image"
docker build -t snmpd:v1 ./snmpd
