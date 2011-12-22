#!/bin/sh

export MAVEN_OPTS="-Xmx512m"

mvn -fn clean install -f onboarding-models/pom.xml
mvn -fn clean install -f onboarding-nexus-p2-runtime/nexus-p2-facade/pom.xml
mvn -fn clean install -f onboarding-nexus-p2-runtime/pom.xml
mvn -fn clean install -f onboarding-nexus/pom.xml
mvn -fn clean install -f onboarding-m2e/pom.xml -Dmaven.test.skip=true

