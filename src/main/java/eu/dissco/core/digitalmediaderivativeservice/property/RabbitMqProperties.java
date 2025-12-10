package eu.dissco.core.digitalmediaderivativeservice.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

  @NotBlank
  private String digitalMediaExchangeName = "digital-media-exchange";

  @NotBlank
  private String digitalMediaRoutingKeyName = "digital-media";

}
