FROM ubuntu:18.04 AS common

LABEL maintainer="Specify Collections Consortium <github.com/specify>"

RUN apt-get update && apt-get -y install --no-install-recommends \
        openjdk-11-jdk-headless \
        maven

RUN groupadd -g 999 specify && \
        useradd -r -u 999 -g specify specify

RUN mkdir -p /home/specify && chown specify.specify /home/specify

COPY --chown=specify:specify . /home/specify/report-runner

USER specify
WORKDIR /home/specify/report-runner

RUN mvn compile

# this causes maven to install jetty so it will be in the container
RUN mvn jetty:help

RUN sed -i s/localhost/0.0.0.0/ jetty.xml

CMD mvn jetty:run
