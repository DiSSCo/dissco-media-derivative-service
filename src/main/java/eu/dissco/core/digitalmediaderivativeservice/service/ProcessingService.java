package eu.dissco.core.digitalmediaderivativeservice.service;

import static eu.dissco.core.digitalmediaderivativeservice.configuration.ApplicationConfiguration.DATE_STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaEvent;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaWrapper;
import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.property.ApplicationProperties;
import eu.dissco.core.digitalmediaderivativeservice.repository.S3Repository;
import eu.dissco.core.digitalmediaderivativeservice.schema.Agent.Type;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMedia;
import eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMediaDerivative;
import eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.DctermsType;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvActivity;
import eu.dissco.core.digitalmediaderivativeservice.utils.AgentUtils;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_STRING).withZone(
      ZoneOffset.UTC);

  private final ObjectMapper objectMapper;
  private final ApplicationProperties properties;
  private final S3Repository s3Repository;
  private final RabbitMqPublisherService rabbitMqPublisherService;

  private static BufferedImage downsizeImage(Pair<Float, Float> dimensions,
      BufferedImage originalImage) {
    var resizedImage = new BufferedImage(dimensions.getLeft().intValue(),
        dimensions.getRight().intValue(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(originalImage, 0, 0, dimensions.getLeft().intValue(),
        dimensions.getRight().intValue(), null);
    graphics2D.dispose();
    return resizedImage;
  }

  private static BufferedImage retrieveImage(DigitalMedia media)
      throws ProcessingFailedException {
    try {
      if (media.getDctermsFormat() != null && media.getDctermsFormat().equals("application/json")) {
        log.info("DigitalMedia {} is of format application/json, skipping processing.",
            media.getId());
        return null;
      }
      var originalImage = ImageIO.read(URI.create(media.getAcAccessURI()).toURL());
      if (originalImage == null) {
        log.error("Could not read image for accessURI: {}", media.getAcAccessURI());
        throw new ProcessingFailedException(
            "Could not read image for accessURI: " + media.getAcAccessURI());
      }
      return originalImage;
    } catch (IOException e) {
      log.error("Error while reading image for accessURI: {}", media.getAcAccessURI(), e);
      throw new ProcessingFailedException(
          "Error while reading image for accessURI: " + media.getAcAccessURI());
    }
  }

  private static DigitalMediaDerivative.DctermsType mapDcTermsType(DigitalMedia media) {
    return DigitalMediaDerivative.DctermsType.fromValue(media.getDctermsType().value());
  }

  public void handleMessage(CreateUpdateTombstoneEvent event)
      throws ProcessingFailedException, JsonProcessingException {
    log.info("Received Provenance: {}", event);
    if (ProvActivity.Type.ODS_CREATE.equals(event.getProvActivity().getType())) {
      var media = retrieveMediaObject(event);
      if (media.getAcAccessURI() == null || media.getAcAccessURI().isBlank()) {
        log.info("DigitalMedia {} does not have an accessURI, skipping processing.",
            media.getId());
        return;
      }
      log.info("Retrieving image for accessURI: {}", media.getAcAccessURI());
      var originalImage = retrieveImage(media);
      if (originalImage != null) {
        var dimension = getDimensions(originalImage);
        var resizedImage = downsizeImage(dimension, originalImage);
        s3Repository.uploadResults(resizedImage, media.getId());
        log.info("Finished uploading {} image", media.getId());
        updateOriginalMedia(media, originalImage);
        setMediaDerivative(media, resizedImage);
        publishDigitalMedia(media);
        log.info("Successfully generated a derivative for DigitalMedia {}", media.getId());
      }
    }
  }

  private void publishDigitalMedia(DigitalMedia media) throws ProcessingFailedException {
    try {
      var wrapper = new DigitalMediaWrapper(media.getType(), media, null);
      var event = new DigitalMediaEvent(Collections.emptySet(), wrapper, false);
      rabbitMqPublisherService.publishDigitalMediaEvent(event);
    } catch (JsonProcessingException ex) {
      log.error("Failed to republish DigitalMedia {}", media.getId(), ex);
      throw new ProcessingFailedException("Failed to republish DigitalMedia " + media.getId());
    }
  }

  private void updateOriginalMedia(DigitalMedia media, BufferedImage originalImage) {
    media
        .withExifPixelXDimension(originalImage.getWidth())
        .withExifPixelYDimension(originalImage.getHeight());
  }

  private void setMediaDerivative(DigitalMedia media, BufferedImage resizedImage) {
    var now = Instant.now();
    var derivative = new DigitalMediaDerivative()
        .withAcAccessURI(properties.getApiUrl() + media.getId() + "/derivative")
        .withDctermsTitle("Derivative of " + media.getId())
        .withDctermsDescription("Image Derivative created by DiSSCo")
        .withExifPixelXDimension(resizedImage.getWidth())
        .withExifPixelYDimension(resizedImage.getHeight())
        .withDctermsCreated(Date.from(now))
        .withOdsHasAgents(List.of(AgentUtils.createMachineAgent(properties.getName(),
            properties.getPid(), "media-derivative-service", DctermsType.DOI,
            Type.SCHEMA_SOFTWARE_APPLICATION)))
        .withDctermsFormat("image/jpeg")
        .withDctermsModified(formatter.format(now))
        .withDctermsRights(media.getDctermsRights())
        .withAcSubtype(media.getAcSubtype())
        .withAcSubjectOrientation(media.getAcSubjectOrientation())
        .withAcSubjectOrientationLiteral(media.getAcSubjectOrientationLiteral())
        .withAcSubtype(media.getAcSubtype())
        .withAcSubtypeLiteral(media.getAcSubtypeLiteral())
        .withDctermsType(mapDcTermsType(media));
    if (media.getOdsHasMediaDerivatives() == null) {
      media.setOdsHasMediaDerivatives(List.of(derivative));
    } else {
      media.getOdsHasMediaDerivatives().add(derivative);
    }
  }

  private DigitalMedia retrieveMediaObject(CreateUpdateTombstoneEvent event)
      throws JsonProcessingException, ProcessingFailedException {
    if (event.getProvEntity().getType().equals("ods:DigitalMedia")) {
      return objectMapper.convertValue(event.getProvEntity().getProvValue(),
          DigitalMedia.class);
    }
    throw new ProcessingFailedException(
        "Invalid provenance entity: " + objectMapper.writeValueAsString(event));
  }

  private Pair<Float, Float> getDimensions(BufferedImage originalImage) {
    var width = originalImage.getWidth();
    var height = originalImage.getHeight();
    var longestSide = Math.max(width, height);
    if (longestSide <= properties.getMaxImageSize()) {
      return Pair.of((float) width, (float) height);
    }
    if (width > height) {
      return Pair.of(properties.getMaxImageSize(), height / (width / properties.getMaxImageSize()));
    } else if (height > width) {
      return Pair.of(width / (height / properties.getMaxImageSize()), properties.getMaxImageSize());
    } else {
      return Pair.of(properties.getMaxImageSize(), properties.getMaxImageSize());
    }
  }

}
