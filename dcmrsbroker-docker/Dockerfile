FROM openjdk:8
MAINTAINER Clifton Li

ENTRYPOINT ["/usr/bin/java", \ 
			"-Ddcmrsbroker.home=/dcmrs-broker", \
			"-jar", \
			"/dcmrs-broker/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar"]

RUN apt-get update && apt-get install -y \
	git \
	maven

RUN git clone https://github.com/RSNA/dcmrs-broker.git

WORKDIR dcmrs-broker

RUN mvn package

ADD dcmrsbroker.properties  /dcmrs-broker/conf/dcmrsbroker.properties

EXPOSE 4567