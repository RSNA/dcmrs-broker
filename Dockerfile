FROM openjdk:8
MAINTAINER Clifton Li

ENTRYPOINT ["/usr/bin/java", \ 
			"-Ddcmrsbroker.home=/dcmrs-broker", \
			"-Dlogback.configurationFile=/dcmrs-broker/conf/logging.xml", \
			"-jar", \
			"/dcmrs-broker/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar"]
			
RUN apt-get update && apt-get install -y \
	git \
	maven

RUN mkdir /dcmrs-broker
COPY . /dcmrs-broker

WORKDIR dcmrs-broker

RUN mkdir cache

RUN mvn package

COPY dcmrsbroker.properties  /dcmrs-broker/conf/dcmrsbroker.properties

COPY logging.xml /dcmrs-broker/conf/logging.xml

EXPOSE 4567 11112