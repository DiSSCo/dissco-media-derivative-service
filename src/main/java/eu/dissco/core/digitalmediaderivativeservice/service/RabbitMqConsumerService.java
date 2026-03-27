package eu.dissco.core.digitalmediaderivativeservice.service;

import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@Slf4j
@AllArgsConstructor
public class RabbitMqConsumerService {

  private final JsonMapper jsonMapper;
  private final ProcessingService processingService;

  @RabbitListener(queues = {
      "${rabbitmq.queue-name:digital-media-derivative-queue}"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessage(@Payload String message) throws ProcessingFailedException {
    var event = jsonMapper.readValue(message, CreateUpdateTombstoneEvent.class);
    processingService.handleMessage(event);
  }

}
