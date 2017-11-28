# dcmrs-broker
DICOM-RS Broker

## Installation
dcmrs-broker.home

dcmrs-broker can be run standalone or as a docker container.  

### Standalone Installation

```
mvn package 
```

Modify the configuation variable in dcmrs-broker.properties. 

Create a home and a conf directory. Copy the properties file to ${dcmrs-broker.home}/conf.

```
i.e mkdir -p /usr/local/dcmrs-broker/conf
```

#### Usage 
```
java -D dcmrsbroker.home=/usr/local/dcmrs-broker ${SOURCE_DIR}/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar

````

### Docker Installation

```
docker build -t rsna/dcmrs-broker .
```

#### Run the image

```
docker run \
    -p 4567:4567 \
    -p 11112:11112 \
    -e QIDO_REMOTE_AE="PARN-DICOM" \
    -e QIDO_REMOTE_HOST="dicom.radiology.ucsf.edu" \
    -e QIDO_REMOTE_PORT="104" \
    -e QIDO_LOCAL_AE="PACS-3502-TESTING" \
    -e WAD0_REMOTE_AE="PARN-DICOM" \
    -e WADO_REMOTE_HOST="dicom.radiology.ucsf.edu" \
    -e WADO_LOCAL_AE="104" \
    -e SCP_LOCAL_AE="PACS-3502-SCP" \
    rsna/dcmrsbroker
```

### Configuration variables

QIDO_REMOTE_AE                              # AE title of the remote device ie. PACS
QIDO_REMOTE_HOST							# Hostname of the remote device
QIDO_REMOTE_PORT							# Port number of the remote device
QIDO_LOCAL_AE								# AE title of the callling device

WAD0_REMOTE_AE								# AE title of the remote device ie. PACS
WADO_REMOTE_HOST							# Hostname of the remote device
WADO_REMOTE_PORT							# Port number of the remote device
WADO_LOCAL_AE								# AE title of the callling device

SCP_LOCAL_AE								# AE title of the callling device
SCP_CACHE_DIR_PATH                          # Directory for cached files

#### Optional Configuation Properties
#### Do not modify if you plan to use default values

QIDO_URL 									# Default /qido-rs

WADO_URL_BASE								# Default /wado-rs
WADO_HTTP_RETRY_AFTER                       # Default 600
WADO_RETRY_DELAY_IN_SECS                    # Default 600
MAX_RETRY_ATTEMPT                           # Default 6
WADO_RETRIEVE_TIMEOUT_IN_SECS               # Default 120
WADO_IGNORE_MISSING_OBJECTS                 # Default false

SCP_LOCAL_PORT								# Default 11112
SCP_CACHE_MAX_AGE_IN_MIN                    # Default 60


### Service URL

http://localhost:4567/qido-rs/studies{?query*,fuzzymatching,limit,offset}
http://localhost:4567/qido-rs/series{?query*,fuzzymatching,limit,offset}
http://localhost:4567/qido-rs/instances{?query*,fuzzymatching,limit,offset}

http://localhost:4567/wado-rs/studies/{StudyInstanceUID}/series/{SeriesInstanceUID}/instances/{SOPInstanceUID}
