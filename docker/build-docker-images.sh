#!/bin/bash
[ ! -d dominion ] && echo "The script must be invoked from the docker subdirectory of the project." && exit 1

POSTGRES=$(docker images | grep postgres | awk '{print $1}')
if [[ -z $POSTGRES ]]; then
  echo "Pulling postgres image from public registry"
  docker pull postgres || exit 1
fi

echo "Builing Minion image"
docker build -t minion:v1 ./minion || exit 1

echo "Builing Dominion image"
docker build -t dominion:v1 ./dominion || exit 1

echo "Building snmpd image"
docker build -t snmpd:v1 ./snmpd || exit 1

echo "Building Tomcat image"
docker build -t tomcat:v1 ./tomcat || exit 1
