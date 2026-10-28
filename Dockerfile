FROM maven:3.10.0-eclipse-temurin-21-noble AS build

LABEL maintainer="Specify Collections Consortium <github.com/specify>"

RUN mkdir -p /tmp/build

WORKDIR /tmp/build

COPY pom.xml /tmp/build

# Do a fake build before copying over the src directory.  This forces
# maven to download all the dependencies so they get cached in a
# docker layer and don't have to be downloaded anytime there is a
# change in the source code.
RUN mvn -q -DskipTests dependency:go-offline

# Do the actual build.
COPY src /tmp/build/src
RUN mvn -B -ntp -DskipTests package

FROM jetty:12.1.14-jdk21-amazoncorretto AS run

# Keep the javax.servlet application on the Java EE 8 deployment environment.
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=ee8-deploy \
    && printf 'environment=ee8\n' > "$JETTY_BASE/webapps/ROOT.properties"

COPY --from=build --chown=jetty:jetty /tmp/build/target/minimal_reports-1.0-SNAPSHOT/ /var/lib/jetty/webapps/ROOT/
