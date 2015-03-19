#!/bin/sh
docker build -t minion:v1 ./minion
docker build -t dominion:v1 ./dominion
docker build -t snmpd:v1 ./snmpd
