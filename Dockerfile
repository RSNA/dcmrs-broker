FROM openjdk:8
MAINTAINER Clifton Li

ENTRYPOINT ["/usr/bin/java", \ 
			"-Ddcmrsbroker.home=/dcmrs-broker", \
			"-jar", \
			"/dcmrs-broker/dcmrsbroker-standalone.jar"]

RUN mkdir /dcmrs-broker
ADD dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar /dcmrs-broker
ADD dcmrsbroker-distribution/target/distribution-binaries/ext /dcmrs-broker/ext
ADD dcmrsbroker.properties  /dcmrs-broker/conf/dcmrsbroker.properties

RUN mkdir /dcmrs-broker/cache

EXPOSE 4567 11112