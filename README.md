# Media Derivative Service

The Media Derivative Service is a service which generates derivative media and stores those with DiSSCo's infrastructure.
There are a couple of major benefits to storing media into DiSSCo's own infrastructure:
- It decouples DiSSCo from the institutional media servers. This will make DiSSCo less dependent on the availability and performance of those servers.
- It reduces load on the institutional media servers, which often have limited bandwidth and are not optimized for high traffic loads.
- It allows DiSSCo to package sub-sections of images for AI training purposes

As financial resources are limited we will one store a derivative at this moment.
This media derivative should be of high enough quality to cover the above-mentioned use-cases.

## Application Flow
1. A request for a derivative media is received by the rabbitMQ consumer.
2. We retrieve the original media from the institutional media server.
3. We generate a derivative media if the size of the image is larger than the configured maximum size.
4. We generate a thumbnail of the media if the size of the image is larger than the configured maximum size.
5. We store the derivative media into a S3 bucket on the DiSSCo Cloud Infrastructure.
6. We update the Digital Media object and add the metadata of the derivative media to it.
7. We publish an event to notify the processing service there is a new version of the Digital Media Object.

## Run locally

To run the system locally, it can be run from an IDEA.
Clone the code and fill in the application properties (see below).
The application requires a connection to rabbitMQ, and a token for the S3 storage.

### Domain Object generation

DiSSCo uses JSON schemas to generate domain objects (e.g. Digital Specimens, Digital Media, etc)
based on the openDS specification. These files are stored in the
`/target/generated-sources/jsonschema2pojo directory`, and must be generated before running locally.
The following steps indicate how to generate these objects.

### Importing Up To-Date JSON Schemas

The JSON schemas are stored in `/resources/json-schemas`. The source of truth for JSON schemas is
the [DiSSCO Schemas Site](https://schemas.dissco.tech/schemas/fdo-type/). If the JSON schema has
changed, the changes can be downloaded using the maven runner script.

1. **Update the pom.xml**: The exec-maven-plugin in the pom indicated which version of the schema to
   download. If the version has changed, update the pom.
2. **Run the exec plugin**: Before the plugin can be run, the code must be compiled. Run the
   following in the terminal (or via the IDE interface):

```
mvn compile 
mvn exec:java
```

### Building POJOs

DiSSCo uses the [JsonSchema2Pojo](https://github.com/joelittlejohn/jsonschema2pojo) plugin to
generate domain objects based on our JSON Schemas. Once the JSON schemas have been updated, you can
run the following from the terminal (or via the IDE interface):

```
mvn clean
mvn jsonschema2pojo:generate
```

## Run as Container

The application can also be run as container.
It will require the environmental values described below.
The container can be built with the Dockerfile, which can be found in the root of the project.

## Configuration
The application can be configured using environment variables. The following variables are available:
- `application.name`: The name of the application, default is DiSSCo Media Derivative Service.
- `application.pid`: The PID of the application.
- `application.max-derivative-image-seze`: The maximum size of the image before a derivative is created, default is 2048 pixels.
- `application.max-thumbnail-image-seze`: The maximum size of the image before a thumbnail is created, default is 400 pixels.

- `spring.rabbitmq.username`: The username to connect to the RabbitMQ server.
- `spring.rabbitmq.password`: The password to connect to the RabbitMQ server.

- `s3.access-key`: The access key to connect to the S3 storage.
- `s3.access-secret`: The access secret to connect to the S3 storage.
- `s3.bucket-name`: The name of the S3 bucket to store the derivative media in.