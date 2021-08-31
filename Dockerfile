FROM ghcr.io/graalvm/graalvm-ce:java11-21.2.0 as graalvm

RUN microdnf -y install wget unzip zip \
    && gu install native-image

RUN mkdir -p /tmp/ssl \
    && cp /usr/lib64/libstdc++.so.6.0.25 /tmp/ssl/libstdc++.so.6 \
    && cp /usr/lib64/libgcc_s.so.1 /tmp/ssl/libgcc_s.so.1 \
    && cp /usr/lib64/libz.so.1 /tmp/ssl/libz.so.1

COPY . /home/app/restservice
WORKDIR /home/app/restservice

RUN \
    # Install SDKMAN
    curl -s "https://get.sdkman.io" | bash; \
    source "$HOME/.sdkman/bin/sdkman-init.sh"; \
    # Install Maven
    sdk install maven; \
    # Install GraalVM Native Image
    gu install native-image;

RUN source "$HOME/.sdkman/bin/sdkman-init.sh" \
    && mvn --version \
    && native-image --version \
    && mvn -DskipTests -Pnative clean package

FROM frolvlad/alpine-glibc

ENV LD_LIBRARY_PATH=/usr/local/lib:/usr/glibc-compat/lib:/opt/libs/lib:/usr/lib:/lib:/lib64

RUN apk -U upgrade \
    && apk add bash
COPY --from=graalvm /tmp/ssl/ /
ENV LD_LIBRARY_PATH /

EXPOSE 8080
COPY --from=graalvm /home/app/restservice/target/graalvm-restservice graalvm-restservice
ENTRYPOINT ["/graalvm-restservice"]