package eu.dissco.core.digitalmediaderivativeservice.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("application")
public class ApplicationProperties {

  @NotBlank
  private String name = "DiSSCo Media Derivative Service";

  @NotBlank
  private String pid = "https://doi.org/Test/Test";

  @Positive
  private float maxImageSize = 2048f;

  @NotBlank
  private String prefix;

  @NotBlank
  private String apiUrl;

}
