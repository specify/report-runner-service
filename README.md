# Specify Report Runner Service
This project implements a simple web service wrapper around the Jasper
Reports libraries for report and label generation in Specify 7.

Requirements
============
* Java JDK >= 1.7
* Apache Maven 2 

Test Service
============
After cloning the repository, build with `mvn compile`. The test
server can be started using the Maven Jetty plugin: `mvn
jetty:run`. The service will run on port `8080` on `localhost`
adapter. These values can be changed in `jetty.xml`. Because there is
no authentication, the report service should run on the same machine
as the Specify 7 server or on a private subnet and only bind an
address that is not routed externally.

In the Specify 7 (development branch) settings file, typically
`specifyweb/settings/local_specify_settings.py`, set the values
`REPORT_RUNNER_HOST` and `REPORT_RUNNER_PORT` appropriately and
restart. A *Reports* task will now be available on the taskbar at the
top of the Specify 7 web application.

Fonts
=====
Unless the report runner service is provided with the fonts used in a
report it will use built-in PDF fonts. The consequences range in
severity from slight differences in appearance to rendered text
overflowing bounding boxes resulting in truncation.

To provide fonts to be used by this service, a font extension jar file
can be added to `src/main/webapp/WEB-INF/lib/` before compiling. There
is a good description of creating the font extension file at
http://javaskeleton.blogspot.com/2010/12/embedding-fonts-into-pdf-generated-by.html
If you follow the iReport instructions in that guide, be sure to
include the variants (italic, bold and bold italic) for each font added. 

Running as a service
=====================
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

Missing Barcode Text
====================
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

