package eu.dissco.core.digitalmediaderivativeservice.service;

import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.MAPPER;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.givenDigitalMediaEvent;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.core.digitalmediaderivativeservice.property.RabbitMqProperties;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class RabbitMqPublisherServiceTest {

  private static RabbitMQContainer container;
  private static RabbitTemplate rabbitTemplate;
  private RabbitMqPublisherService rabbitMqPublisherService;

  @BeforeAll
  static void setupContainer() throws IOException, InterruptedException {
    container = new RabbitMQContainer("rabbitmq:4.2-management-alpine");
    container.start();
    container.execInContainer("rabbitmqadmin", "declare", "exchange", "name=digital-media-exchange",
        "type=direct", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "queue", "name=digital-media-queue",
        "queue_type=quorum", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "binding",
        "source=digital-media-exchange",
        "destination_type=queue", "destination=digital-media-queue", "routing_key=digital-media");
    CachingConnectionFactory factory = new CachingConnectionFactory(container.getHost());
    factory.setPort(container.getAmqpPort());
    factory.setUsername(container.getAdminUsername());
    factory.setPassword(container.getAdminPassword());
    rabbitTemplate = new RabbitTemplate(factory);
    rabbitTemplate.setReceiveTimeout(100L);
  }

  @AfterAll
  static void shutdownContainer() {
    container.stop();
  }

  @BeforeEach
  void setup() {
    rabbitMqPublisherService = new RabbitMqPublisherService(MAPPER, rabbitTemplate,
        new RabbitMqProperties());
  }

  @Test
  void testPublishCreateEvent() throws JsonProcessingException {
    // Given

    // When
    rabbitMqPublisherService.publishDigitalMediaEvent(givenDigitalMediaEvent());

    // Then
    var message = rabbitTemplate.receive("digital-media-queue");
    assertThat(new String(message.getBody())).isNotNull();
  }
}
