# Specify Report Runner Service

This project implements a simple web service wrapper around the Jasper
Reports libraries for report and label generation in Specify 7.

## Requirements

- Java JDK 21
- Apache Maven 3.9.6 or newer

The production image uses Jetty 12.1 with Java 21 and the Java EE 8
deployment environment. This preserves the application's `javax.servlet`
API, `/report` endpoint, existing JasperReports 6 templates, 100 MB form
limit, and `/var/lib/jetty/webapps/ROOT/WEB-INF/lib/` custom-font mounts.
JasperReports, Batik, and XML Graphics Commons were changed in this PR; their
security updates still require a separate dependency review.

## Test Service

After cloning the repository, build and run integration tests with
`mvn verify`. The tests start the packaged application on Jetty EE8,
submit JRXML, data, and parameters, and verify the text in the resulting
PDF. They also decode a QR code rendered in the PDF and check a form
larger than Jetty's default 200 KB limit.
The test server can be started using the Maven Jetty plugin: `mvn
jetty:run`. The service will run on port `8080` on `localhost`
adapter. These values can be changed in the Jetty plugin's `httpConnector`
configuration in `pom.xml`. Because there is
no authentication, the report service should run on the same machine
as the Specify 7 server or on a private subnet and only bind an
address that is not routed externally.

In the Specify 7 (development branch) settings file, typically
`specifyweb/settings/local_specify_settings.py`, set the values
`REPORT_RUNNER_HOST` and `REPORT_RUNNER_PORT` appropriately and
restart. A *Reports* task will now be available on the taskbar at the
top of the Specify 7 web application.

## Fonts

Unless the report runner service is provided with the fonts used in a
report it will use built-in PDF fonts. The consequences range in
severity from slight differences in appearance to rendered text
overflowing bounding boxes resulting in truncation to report runner throwing
exceptions.

To provide fonts to be used by this service, a font extension jar file
can be added to `src/main/webapp/WEB-INF/lib/` before compiling. There
is a good description of creating the font extension file at
http://javaskeleton.blogspot.com/2010/12/embedding-fonts-into-pdf-generated-by.html
If you follow the iReport instructions in that guide, be sure to
include the variants (italic, bold and bold italic) for each font added.

If using the report runner in a Docker container, the font extension
jar can be mounted into the container using a volume directive such
as:

```
docker run -p 8080 --name report-runner -d -v ./report-fonts.jar:/var/lib/jetty/webapps/ROOT/WEB-INF/lib/report-fonts.jar specifyconsortium/report-runner
```

## Deployment (Docker Compose)

This repository includes a `docker-compose.yml` file that replaces
manual `docker run` commands.

### 1. Build and start

Copy `.env.example` to `.env` and update values for your host:

```
cp .env.example .env
```

Then edit `.env` in the repository root to set deployment values:

```
REPORT_RUNNER_BIND_IP=172.31.30.34
REPORT_RUNNER_PORT=8080
```

You will likely want to add [custom fonts](https://discourse.specifysoftware.org/t/specify-report-runner-fonts/1659), which you can do so by adding a
Jasper font-extension jar file to either to your `.env` file as a path, or by directly mounting it as a file in the `docker-compose.yml` file. By default,
there are only two volumes commented, one for [standard fonts](https://discourse.specifysoftware.org/t/specify-report-runner-fonts/1659#p-3113-adding-fonts-2) and
one for Google Fonts, which need to be [packaged manually using Jaspersoft Studio](https://discourse.specifysoftware.org/t/adding-custom-fonts-to-the-report-runner/2809).

If you add these fonts, make sure to uncomment the lines in the `docker-compose.yml` file:
```yml
    volumes:
      - "${FONTS_JAR}:/var/lib/jetty/webapps/ROOT/WEB-INF/lib/report-fonts.jar:ro"
      - "${GOOGLE_FONTS_JAR}:/var/lib/jetty/webapps/ROOT/WEB-INF/lib/google-fonts.jar:ro"
```

Then start and build from the main directory:

```
docker compose up -d --build
```

### Runtime updates and validation

Refresh base images when building a security update:

```bash
docker compose build --pull --no-cache report-runner
docker compose up -d --no-deps report-runner
docker compose exec report-runner java -version
docker compose logs --tail=100 report-runner
```

The builder is pinned to Maven 3.10.0 / Java 21 and the runtime to Jetty
12.1.14 / Java 21. Update the Jetty image tag and `jetty.version` in
`pom.xml` together when adopting a new Jetty patch. A rebuild refreshes
the Java 21 base available under that tag, but cannot fix dependencies
whose versions remain pinned to vulnerable releases.

Before production deployment, test representative collection reports and
labels, including barcodes and custom fonts. Record the new image digest
and confirm scanner findings against that image. Keep the old image in a
controlled rollback location until validation completes; after the cutover,
remove retired local images through Docker rather than deleting its storage
directories. Rollback to a Java 8 image restores its old vulnerabilities.

Publishing a replacement image does not update a running KU container.
From its existing Specify Cloud Compose directory, pull and recreate only
the report runner after the new image is published:

```bash
docker compose pull report-runner
docker compose up -d --no-deps report-runner
docker compose exec report-runner java -version
```

### 2. Stop or restart

```
docker compose stop
docker compose start
docker compose down
```

### Use Docker Hub Image

For production environments, it is not necessary to build the image yourself.
You can simply fetch it from Docker Hub with a configuration like this one:

```yml
services:
  report-runner:
    container_name: report-runner
    image: specifyconsortium/report-runner # No building needed!
    restart: unless-stopped
    ports:
      - "${REPORT_RUNNER_BIND_IP}:${REPORT_RUNNER_PORT}:8080"
```

## Running as a service

Use the following *SystemD* script to start report runner service

PS: change User and home path to your needs.

```
[Unit]
Description=Specify Report Runner Service 
# Author Job Diogenes Ribeiro Borges
Wants=network.target
ConditionPathExists=/home/specify/report-runner-service

[Service]
User=specify
WorkingDirectory=/home/specify/report-runner-service
ExecStart=/usr/bin/mvn jetty:run

[Install]
Alias=ireportrunner.service
```

## Missing Barcode Text

When running with a headless JRE, there is a bug in the barbecue
barcode generation library that prevents the text value of a barcode
from be included even if that option is turned on in the report
definition. The bug has been
[fixed](https://github.com/barbecue-sf/barbecue/commit/420f362ac2348b8a7cbb056e5d920317ce0a0ce1)
in the
[barbecue repository](https://github.com/barbecue-sf/barbecue) in an
unreleased version, *1.9-dev*. The solution is to clone the barbecue
repository locally and build and install the maven artifact.

```
git clone https://github.com/barbecue-sf/barbecue.git
cd barbecue/barbecue
mvn compile
mvn install
```

Then update the `pom.xml` in this repository to depend on version
*1.9-dev* instead of the currently released *1.5-beta1*.

