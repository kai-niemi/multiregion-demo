#!/bin/bash

args="\
--url jdbc:postgresql://localhost:26257/demo?sslmode=disable \
--user root"

jarfile=demo.jar

if [ ! -f "$jarfile" ]; then
    jarfile=target/demo.jar
fi

java -jar $jarfile $args $*