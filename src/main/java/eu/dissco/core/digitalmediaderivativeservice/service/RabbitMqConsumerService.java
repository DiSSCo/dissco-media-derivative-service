package eu.dissco.core.digitalmediaderivativeservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class RabbitMqConsumerService {

  private final ObjectMapper mapper;
  private final ProcessingService processingService;

  @RabbitListener(queues = {
      "${rabbitmq.queue-name:digital-media-derivative-queue}"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessage(@Payload String message) throws IOException, ProcessingFailedException {
    var event = mapper.readValue(message, CreateUpdateTombstoneEvent.class);
    processingService.handleMessage(event);
  }

}
