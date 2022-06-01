#!/usr/bin/env bash

# Maven build
echo -ne "Building Maven project ... "
mvn clean package > /dev/null 2>&1
echo "DONE."

# # Gradle build
# echo -ne "Building Gradle project ... "
# gradle clean build > /dev/null 2>&1
# echo "DONE."

# #If you have 'pv' installed, this option displays a progress bar
# echo "Building package ... "
# mvn clean package | (pv -p -t -s 20K) | (cat > /dev/null)
# echo "DONE."

# Maven build
echo -ne "Building Native Image Executable (please be patient) ... "
mvn package -Pnative > /dev/null 2>&1
echo "DONE."

# # Gradle build
# echo -ne "Building Native Image Executable (please be patient) ... "
# gradle nativeCompile > /dev/null 2>&1
# echo "DONE."

# # If you have 'pv' installed, this option displays a progress bar
# echo "Building Native Image Executable ... "
# mvn package -Pnative | (pv -p -t -s 55K) | (cat > /dev/null)
# echo "DONE."

# Maven build
echo -ne "Building Static Native Image Executable (please be patient) ... "
mvn package -Pstatic > /dev/null 2>&1
echo "DONE."


echo -ne "Building containers ...\n"
docker login container-registry.oracle.com
echo -ne "Building JVM container ... "
docker build -f ./Dockerfile.jvm -t localhost/rest-service-demo:jvm . > /dev/null 2>&1
echo "Done."
echo -ne "Building Native Image container ... "
docker build -f ./Dockerfile.native -t localhost/rest-service-demo:native . > /dev/null 2>&1
echo "Done."
echo -ne "Multistage build, please be patient ... "
docker build -f ./Dockerfile.stage -t localhost/rest-service-demo:stage . > /dev/null 2>&1
echo "Done."
echo -ne "Building Distroless container ... "
docker build -f ./Dockerfile.distroless -t localhost/rest-service-demo:distroless . > /dev/null 2>&1
echo "Done."
echo -ne "Building Static Native Image container ... "
docker build -f ./Dockerfile.static -t localhost/rest-service-demo:static . > /dev/null 2>&1
echo "Done."
echo ""
echo "Build complete!"
echo ""
echo "See README for instructions on how to run the application and individual containers."