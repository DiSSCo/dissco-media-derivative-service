package eu.dissco.core.digitalmediaderivativeservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaEvent;
import eu.dissco.core.digitalmediaderivativeservice.property.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final ObjectMapper mapper;
  private final RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitMqProperties;

  public void publishDigitalMediaEvent(DigitalMediaEvent digitalMediaEvent)
      throws JsonProcessingException {
    rabbitTemplate.convertAndSend(rabbitMqProperties.getDigitalMediaExchangeName(),
        rabbitMqProperties.getDigitalMediaRoutingKeyName(),
        mapper.writeValueAsString(digitalMediaEvent));
  }

}
