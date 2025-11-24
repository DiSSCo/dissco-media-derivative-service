package eu.dissco.core.digitalmediaderivativeservice.service;

import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.MAPPER;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.givenProvenanceEventJson;

import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerServiceTest {

  @Mock
  private ProcessingService processingService;

  private RabbitMqConsumerService rabbitMqConsumerService;

  @BeforeEach
  void setup() {
    this.rabbitMqConsumerService = new RabbitMqConsumerService(MAPPER, processingService);
  }

  @Test
  void testGetMessage() throws IOException, ProcessingFailedException {
    // Given
    var message = MAPPER.writeValueAsString(givenProvenanceEventJson());

    // When / Then
    rabbitMqConsumerService.getMessage(message);
  }

}