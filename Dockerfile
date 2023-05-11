FROM arm64v8/maven:3.9.1-eclipse-temurin-8 AS build-report-runner

LABEL maintainer="Specify Collections Consortium <github.com/specify>"

RUN mkdir -p /tmp/build

WORKDIR /tmp/build

COPY pom.xml /tmp/build

# Do a fake build before copying over the src directory.  This forces
# maven to download all the dependencies so they get cached in a
# docker layer and don't have to be downloaded anytime there is a
# change in the source code.
RUN mvn compile && mvn war:exploded

# Do the actual build.
COPY src /tmp/build/src
RUN mvn compile && mvn war:exploded

FROM arm64v8/jetty:9.4.51-jdk8-eclipse-temurin AS run-report-runner

COPY --from=build-report-runner /tmp/build/target/minimal_reports* /var/lib/jetty/webapps/ROOT
