package eu.dissco.core.digitalmediaderivativeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DigitalMediaDerivativeServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DigitalMediaDerivativeServiceApplication.class, args);
  }

}
