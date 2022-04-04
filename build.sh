#!/usr/bin/env bash

VER=0.1

echo -ne "Building package ..."
mvn clean package > /dev/null 2>&1
echo " DONE."

# If you have 'pv' installed, this option displays a progress bar
# echo "Building package ..."
# mvn clean package | (pv -p -t -s 20K) | (cat > /dev/null)
# echo "DONE."

echo -ne "Building Native Image Executable (please be patient) ..."
mvn package -Pnative > /dev/null 2>&1
echo " DONE."

# # If you have 'pv' installed, this option displays a progress bar
# echo "Building Native Image Executable ..."
# mvn package -Pnative | (pv -p -t -s 55K) | (cat > /dev/null)
# echo "DONE."

echo -ne "Building containers (please be patient) ..."
docker login container-registry.oracle.com > /dev/null 2>&1
docker build -f ./Dockerfile.jvm -t localhost/rest-service-demo:jvm.${VER} . > /dev/null 2>&1

docker build -f ./Dockerfile.native -t localhost/rest-service-demo:native.${VER} . > /dev/null 2>&1

docker build -f ./Dockerfile.stage -t localhost/rest-service-demo:stage.${VER} . > /dev/null 2>&1

docker build -f ./Dockerfile.distroless -t localhost/rest-service-demo:distroless.${VER} . > /dev/null 2>&1

echo " DONE."

echo "See README for instructions on how to run the application and individual containers."