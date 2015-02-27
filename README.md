# Specify Report Runner Service
This project implements a simple web service wrapper around the Jasper Reports libraries for report and label generation in Specify 7.

Requirements
============
* Java JDK >= 1.7
* Apache Maven 2 

Test Service
============
After cloning the repository, build with `mvn compile`. The test server can be started using the Maven Jetty plugin: `mvn jetty:run`. The service will run on port `8080` on `localhost` adapter. These values can be changed in `jetty.xml`. Because there is no authentication, the report service should run on the same machine as the Specify 7 server or on a private subnet and only bind an address that is not routed externally.

In the Specify 7 (development branch) settings file, typically `specifyweb/settings/local_specify_settings.py`, set the values `REPORT_RUNNER_HOST` and `REPORT_RUNNER_PORT` appropriately and restart. A *Reports* task will now be available on the taskbar at the top of the Specify 7 web application.

Fonts
=====
Unless the report runner service is provided with the fonts used in a report it will use built-in PDF fonts. The consequences range in severity from slight differences in appearance to rendered text overflowing bounding boxes resulting in truncation.

Instructions for creating a jar file providing all necessary fonts will be forthcoming.

