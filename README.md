[![Build Status](https://img.shields.io/travis/valmaev/antforce/master.svg)](https://travis-ci.org/valmaev/antforce)
[![Coverage Status](https://img.shields.io/codecov/c/github/valmaev/antforce/master.svg)](https://codecov.io/gh/valmaev/antforce)
[![GitHub Release](https://img.shields.io/github/release/valmaev/antforce.svg)](https://github.com/valmaev/antforce/releases/latest)
[![JDK Version](https://img.shields.io/badge/jdk-1.8+-A92B7D.svg)](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[![Ant Version](https://img.shields.io/badge/ant-1.9.7+-A92B7D.svg)](https://ant.apache.org)

#AntForce

**AntForce** is a set of [Apache Ant](https://ant.apache.org) tasks that help implementing Continuous Integration for Force.com projects. It's built on top of [Force.com Migration Tool](https://developer.salesforce.com/docs/atlas.en-us.daas.meta/daas) and extend it in many ways.

##Features
- deploy with reports
    - precise code coverage calculation
        - AntForce overcomes the issue of Metadata API which doesn't return coverage data for classes that don't have any tests
    - wildcards support for test classes
    - multiple types of reports: 
        - JUnit
        - Cobertura
        - HTML coverage report with or without code highlighting
- execute anonymous Apex code
- (un)install managed packages
- integration with build servers: Jenkins, TeamCity, TFS, Visual Studio Team System
- ability to use in parallel with Force.com Migration Tool in same build script

##Requirements

- JDK 1.8+
- Ant 1.9.7+

##Delivery

AntForce delivered through [Bintray](https://bintray.com/valmaev/maven/antforce/_latestVersion). It means that:

- you don't need to store any build-related .jar-files inside your repo! 
- you can use dependency manager such as [Apache Ivy](https://ant.apache.org/ivy) to download AntForce right in process of your projects's build.

##Versioning

AntForce follows [Semantic Versioning v2.0.0](http://semver.org/spec/v2.0.0.html).

##Credits
AntForce uses code or/and ideas from following open source projects:

- [Deployment Tools from FinancialForce](https://github.com/financialforcedev/df12-deployment-tools)
- [DeployWithXmlReportTask](https://code.google.com/archive/p/force-deploy-with-xml-report-task)
- [Enforce Gradle plugin](https://github.com/fundacionjala/enforce-gradle-plugin)
- [Istanbul](https://github.com/gotwarlost/istanbul)
- [Google Code Prettify](https://github.com/google/code-prettify)
