FROM maven AS build

LABEL maintainer="Specify Collections Consortium <github.com/specify>"

RUN mkdir -p /tmp/build

WORKDIR /tmp/build

COPY pom.xml /tmp/build

# Do a fake build before copying over the src directory.  This forces
# maven to download all the dependencies so they get cached in a
# docker layer and don't have to be downloaded anytime there is a
# change in the source code.
RUN mvn -DskipTests package

# Do the actual build.
COPY src /tmp/build/src
RUN mvn -DskipTests package

FROM jetty:9.4-jre8 AS run

COPY --from=build /tmp/build/target/*.war /var/lib/jetty/webapps/ROOT.war
