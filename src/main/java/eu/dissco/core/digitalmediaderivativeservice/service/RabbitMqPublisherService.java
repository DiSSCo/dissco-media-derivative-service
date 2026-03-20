package eu.dissco.core.digitalmediaderivativeservice.service;

import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaEvent;
import eu.dissco.core.digitalmediaderivativeservice.property.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final JsonMapper jsonMapper;
  private final RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitMqProperties;

  public void publishDigitalMediaEvent(DigitalMediaEvent digitalMediaEvent) {
    rabbitTemplate.convertAndSend(rabbitMqProperties.getDigitalMediaExchangeName(),
        rabbitMqProperties.getDigitalMediaRoutingKeyName(),
        jsonMapper.writeValueAsString(digitalMediaEvent));
  }

}
