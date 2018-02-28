# DICOM-RS Broker


A web application for adding [DICOM-RS](https://dicomweb.hcintegrations.ca/) support to legacy PACS archives.  

The broker currently supports translating [QIDO-RS](http://medical.nema.org/medical/dicom/current/output/chtml/part18/sect_6.7.html) and [WADO-RS](http://dicom.nema.org/medical/dicom/current/output/chtml/part18/sect_6.5.html) requests to the corresponding C-FIND and C-MOVE transactions.  

## Running the Application

The DICOM RS broker can be run **standalone** or as a **docker container**.  

### Standalone Installation

#### Build project


* ` mvn package `

#### Setup configuration

Create a home directory with a `conf` subdirectory for the configuration files. For example: 

* `mkdir -p /usr/local/dcmrs-broker/conf`

Copy the sample `dcmrs-broker.properties` properties file to the `conf` subdirectory you just created. For example:

* `cp ${SOURCE_DIR}/dcmrsbroker.properties  /usr/local/dcmrs-broker/conf`

Modify the properties in the `dcmrs-broker.properties` file to match your environment (see definition of configuration options below). 

#### Launch app

Run the app using `java`. For example: 

```
java \
	-Ddcmrsbroker.home=/usr/local/dcmrs-broker \
	${SOURCE_DIR}/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar
```


####  Logging configuration (optional)

The broker utilizes Logback for logging. By default the configuration is set to log INFO level events to the console.  To override this behavior create a Logback configuration file and pass the location as a Java system property named `logback.configurationFile`.  For example:

For example if you created a configuration file at `/usr/local/dcmrs-broker/conf/logging.xml`, the `java` command would look like:

```
java \
	-Ddcmrsbroker.home=/usr/local/dcmrs-broker \
	-Dlogback.configurationFile=/usr/local/dcmrs-broker/conf/logging.xml \
	${SOURCE_DIR}/dcmrsbroker-distribution/target/distribution-binaries/dcmrsbroker-standalone.jar
```

For more information see the [Logback Documentation](https://logback.qos.ch/manual/configuration.html#configFileProperty).

### Docker Installation

#### Run the image

Note: you will need to modify the environment variables to match your setup (see definition of configuration options below). 

```
docker run \
    -p 4567:4567 \
    -p 11112:11112 \
    -e QIDO_REMOTE_AE="CFIND-SCP" \
    -e QIDO_REMOTE_HOST="dicom.example.com" \
    -e QIDO_REMOTE_PORT="104" \
    -e QIDO_LOCAL_AE="DCMRS-BROKER-SCU" \
    -e WADO_REMOTE_AE="CMOVE-SCP" \
    -e WADO_REMOTE_HOST="dicom.example.com" \
    -e WADO_REMOTE_PORT="104" \
    -e WADO_LOCAL_AE="DCMRS-BROKER-SCU" \
    -e SCP_LOCAL_AE="DCMRS-BROKER-SCP" \
    -e SCP_CACHE_DIR_PATH=/dcmrs-broker/cache \
    rsna/dcmrs-broker
```

####  Logging configuration (optional)
The broker utilizes Logback for logging. By default the configuration is set to log INFO level events to the console.  To override this behavior create a Logback configuration file and mount it to `/dcmrs-broker/conf/logging.xml`.  

For example if you created a configuration file at `/usr/local/custom-logging.xml`, the `docker run` command would look like:

```
docker run \
    -p 4567:4567 \
    -p 11112:11112 \
    -e QIDO_REMOTE_AE="CFIND-SCP" \
    -e QIDO_REMOTE_HOST="dicom.example.com" \
    -e QIDO_REMOTE_PORT="104" \
    -e QIDO_LOCAL_AE="DCMRS-BROKER-SCU" \
    -e WADO_REMOTE_AE="CMOVE-SCP" \
    -e WADO_REMOTE_HOST="dicom.example.com" \
    -e WADO_REMOTE_PORT="104" \
    -e WADO_LOCAL_AE="DCMRS-BROKER-SCU" \
    -e SCP_LOCAL_AE="DCMRS-BROKER-SCP" \
    -e SCP_CACHE_DIR_PATH=/dcmrs-broker/cache \
    -v /usr/local/cache:/dcmrs-broker/cache \
    -v /usr/local/custom-logging.xml:/dcmrs-broker/conf/logging.xml \
    rsna/dcmrs-broker
```

For more information see the [Logback Documentation](https://logback.qos.ch/manual/configuration.html).

## Configuration 

### Required Properties
The following properties are required for the broker to run:

Java Property Name|Docker Environment Variable Name|Description|Default Value
-|-|-|-
qido.remote_ae|QIDO_REMOTE_AE|AE title of the SCP that services C-FIND requests (i.e. called AE title)|N/A
qido.remote_host|QIDO_REMOTE_HOST|host or IP address of the SCP that services C-FIND requests|N/A
qido.remote_port|QIDO_REMOTE_PORT|port of the SCP that services C-FIND requests|11112
qido.local_ae|QIDO_LOCAL_AE|AE title used by the broker when making C-FIND requests (i.e. calling AE title)|N/A
wado.remote_ae|WADO_REMOTE_AE|AE title of the SCP that services C-MOVE requests (i.e. called AE title)|N/A
wado.remote_host|WADO_REMOTE_HOST|host or IP address of the SCP that services C-MOVE requests|N/A
wado.remote_port|WADO_REMOTE_PORT|port of the SCP that services C-MOVE requests|11112
wado.local_ae|WADO_LOCAL_AE|AE title used by the broker when making C-MOVE requests (i.e. calling AE title)|N/A
scp.local_ae|SCP_LOCAL_AE|AE title of the C-STORE SCP used for receiving images (i.e. called AE title)|NA
scp.local_port|SCP_LOCAL_PORT|the port the SCP will listen on|11112
scp.cache_dir_path|SCP_CACHE_DIR_PATH|Path to directory for cached files|`/dcmrs-broker/cache` in docker image



### Optional Properties
The following properties are available to modify the default behavior of the broker:

Java Property Name|Docker Environment Variable Name|Description|Default Value
-|-|-|-
qido.url_base|QIDO_URL_BASE|The base URL for QIDO requests|`/qido-rs`
wado.url_base|WADO_URL_BASE|The base URL for WADO requests|`/wado-rs`
wado.http_retry_after|WADO_HTTP_RETRY_AFTER|The value (in secs) to include in the HTTP `Retry-After` header in a `503` response|600
wado.max_retry_attempts|WADO_MAX_RETRY_ATTEMPTS|The number of times the broker should retry failed C-MOVE requests|6
wado.retry_delay_in_secs|WADO_RETRY_DELAY_IN_SECS|The number of seconds the broker should wait between retrying failed C-MOVE requests|600
wado.retrieve_timeout_in_secs|WADO_RETRIEVE_TIMEOUT_IN_SECS|The number of seconds to wait after a C-MOVE request has completed for all images to arrive |120
wado.ignore_missing_objects|WADO_IGNORE_MISSING_OBJECTS|Flag indicating if the broker should require the numbers of images received match the number of images indicated in the C-MOVE response.  |false
scp.cache_max_age_in_min| SCP_CACHE_MAX_AGE_IN_MIN|The number of minutes the broker should store a study in its local cache|60




## Example Usage with curl

### QIDO
```
curl -H "Accept: application/json" http://localhost:4567/qido-rs/studies{?query*,fuzzymatching,limit,offset}
```


### WADO
```
curl -H "Accept:  multipart/related; type=\"application/dicom\"" \
        http://localhost:4567/wado-rs/studies/{StudyInstanceUID}
```

Note: Currently only the retrieval of DICOM Part 10 objects is supported. The retrieval of bulk data and meta data are not supported. 
