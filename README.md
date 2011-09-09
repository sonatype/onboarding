# Overview

This is the latest development snapshot of Sonatype Onboarding technology.

# Build environment setup

Install or generate a keypair which will be used to sign JNLP-based Eclipse
installer generated during the build. For testing purposes self-signed 
certificate is sufficient and the following command can be used to generate
it. 

	keytool -genkeypair

See http://java.sun.com/javase/6/docs/technotes/tools/windows/keytool.html
for me information about keytool.

Once kaypair has been installed or generated, information about it should be
passed to the build using -Djarsigner.storepass and -Djarsigner.alias 
mvn invocation properties. Alternatively, these properties can be configured
in settings.xml, for example,

	<settings>
	  <profiles>
	    <profile>
	      <id>development</id>
	      <properties>
	        <jarsigner.storepass>test123</jarsigner.storepass>
	        <jarsigner.alias>mykey</jarsigner.alias>
	      </properties>
	    </profile>
	  </profiles>
	  <activeProfiles>
	    <activeProfile>development</activeProfile>
	  </activeProfiles>
	</settings>

# Building

Use the following command to build everything. The command assumes that 
Java 5+ and mvn 3.0+ are available on PATH.

	./build.sh

