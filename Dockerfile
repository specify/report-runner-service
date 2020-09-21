FROM maven AS build

LABEL maintainer="Specify Collections Consortium <github.com/specify>"

RUN mkdir -p /tmp/build

COPY . /tmp/build

WORKDIR /tmp/build

RUN mvn package

FROM jetty AS run

COPY --from=build /tmp/build/target/*.war /var/lib/jetty/webapps/ROOT.war
