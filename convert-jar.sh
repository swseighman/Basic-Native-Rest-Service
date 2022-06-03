#!/usr/bin/env bash

rm -rf target/native
mkdir -p target/native
cd target/native
jar -xvf ../rest-service-demo-0.0.1-SNAPSHOT-exec.jar >/dev/null 2>&1
cp -R META-INF BOOT-INF/classes
native-image -H:Name=rest-service-demo-sh -cp BOOT-INF/classes:`find BOOT-INF/lib | tr '\n' ':'`
mv rest-service-demo-sh ../