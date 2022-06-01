#!/usr/bin/env bash

echo -ne "Building containers (please be patient)\n"
docker login container-registry.oracle.com
docker build -f ./Dockerfile.jvm -t localhost/rest-service-demo:jvm . > /dev/null 2>&1
echo "JVM built ..."
docker build -f ./Dockerfile.native -t localhost/rest-service-demo:native . > /dev/null 2>&1
echo "Native built ..."
docker build -f ./Dockerfile.stage -t localhost/rest-service-demo:stage . > /dev/null 2>&1
echo "Multistage built ..."
docker build -f ./Dockerfile.distroless -t localhost/rest-service-demo:distroless . > /dev/null 2>&1
echo "Distroless built ..."
docker build -f ./Dockerfile.static -t localhost/rest-service-demo:static . > /dev/null 2>&1
echo "Static built ..."
echo ""
echo " DONE."

echo "See README for instructions on how to run the application and individual containers."