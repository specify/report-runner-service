FROM maven AS build

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

FROM jetty AS run

COPY --from=build /tmp/build/target/minimal_reports* /var/lib/jetty/webapps/ROOT
