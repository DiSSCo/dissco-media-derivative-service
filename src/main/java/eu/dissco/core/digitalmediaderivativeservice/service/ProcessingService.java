package eu.dissco.core.digitalmediaderivativeservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.property.ApplicationProperties;
import eu.dissco.core.digitalmediaderivativeservice.repository.S3Repository;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMedia;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

  private final ObjectMapper objectMapper;
  private final ApplicationProperties properties;
  private final S3Repository s3Repository;

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

  public void handleMessage(CreateUpdateTombstoneEvent event)
      throws IOException, ProcessingFailedException {
    log.info("Received CreateUpdateTombstoneEvent: {}", event);
    var media = retrieveMediaObject(event);
    log.info("Retrieving image for accessURI: {}", media.getAcAccessURI());
    var originalImage = ImageIO.read(URI.create(media.getAcAccessURI()).toURL());
    var dimensions = getDimensions(originalImage);
    var resizedImage = downsizeImage(dimensions, originalImage);
    s3Repository.uploadResults(resizedImage, media.getId());
    log.info("Finished uploading {} images", media.getId());
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
