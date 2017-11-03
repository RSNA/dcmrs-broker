#dcmrsdocker-docker
Docker distribution of the DICOM-RS Broker

## Installation

Modify dcmrsbroker.properties. It will be copied into the docker image during the build process.

### Build the image

```
docker build -t "rsna/dcmrsbroker" <Dockerfile>
```

### Run the image

```
docker run -d -p 4567:4567 rsna/dcmrsbroker
```