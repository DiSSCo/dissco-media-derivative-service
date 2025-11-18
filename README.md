# Media Derivative Service

The Media Derivative Service is a service which generates derivative media and stores those with DiSSCo's infrastructure.
There are a couple of major benefits to storing media into DiSSCo's own infrastructure:
- It decouples DiSSCo from the institutional media servers. These servers are notoriously unreliable, impacting DiSSCo each time media become unavailable.
- It reduces load on the institutional media servers, which often have limited bandwidth and are not optimized for high traffic loads.
- It allows DiSSCo to package cross-sections of images for AI training purposes

As financial resources are limited we will one store a derivative at this moment.
This derivative should be of high enough quality to cover the above-mentioned use-cases.

## Application Flow
1. A request for a derivative media is received by the rabbitMQ consumer.
2. We retrieve the original media from the institutional media server.
3. We generate a derivative media if the size of the image is larger than the configured maximum size.
4. We store the derivative media into a S3 bucket on the DiSSCo Cloud Infrastructure.
5. We update the Digital Media object and add the metadata of the derivative media to it.
6. We publish an event to notify the processing service there is a new version of the Digital Media Object.

## Configuration
The application can be configured using environment variables. The following variables are available:
- `application.name`: The name of the application, default is DiSSCo Media Derivative Service.
- `application.pid`: The PID of the application.
- `application.max-image-size`: The maximum size of the image before a derivative is created, default is 2048 pixels.

- `spring.rabbitmq.username`: The username to connect to the RabbitMQ server.
- `spring.rabbitmq.password`: The password to connect to the RabbitMQ server.


- `s3.access-key`: The access key to connect to the S3 storage.
- `s3.access-secret`: The access secret to connect to the S3 storage.
- `s3.bucket-name`: The name of the S3 bucket to store the derivative media in.