FROM openjdk:8
MAINTAINER Clifton Li

ENTRYPOINT ["/usr/bin/java", \ 
			"-Ddcmrsbroker.home=/dcmrs-broker", \
			"-jar", \
			"/dcmrs-broker/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar"]
			
RUN apt-get update && apt-get install -y \
	git \
	maven

RUN mkdir /dcmrs-broker
ADD . /dcmrs-broker

WORKDIR dcmrs-broker

RUN mvn package

ADD dcmrsbroker.properties  /dcmrs-broker/conf/dcmrsbroker.properties

RUN mkdir /dcmrs-broker/cache

EXPOSE 4567 11112