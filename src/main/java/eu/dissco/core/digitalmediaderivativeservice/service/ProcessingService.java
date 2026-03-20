package eu.dissco.core.digitalmediaderivativeservice.service;

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
import java.util.ArrayList;
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

  public static final String DOI_PROXY = "https://doi.org/";

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

  public void handleMessage(CreateUpdateTombstoneEvent event)
      throws ProcessingFailedException, JsonProcessingException {
    if (ProvActivity.Type.ODS_CREATE.equals(event.getProvActivity().getType())) {
      log.info("Received Provenance: {}", event);
      var media = retrieveMediaObject(event);
      log.info("Retrieving image for accessURI: {}", media.getAcAccessURI());
      var originalImage = retrieveImage(media);
      if (originalImage != null) {
        var derivativeImage = storeImage(originalImage, media, false);
        var thumbnailImage = storeImage(originalImage, media, true);
        updateOriginalMedia(media, originalImage);
        setMediaDerivative(media, derivativeImage, false);
        setMediaDerivative(media, thumbnailImage, true);
        publishDigitalMedia(media);
        log.info("Successfully generated a derivative for DigitalMedia {}", media.getId());
      }
    } else {
      log.debug("Received non-create event, skipping processing: {}", event);
    }
  }

  private BufferedImage storeImage(BufferedImage originalImage, DigitalMedia media,
      boolean isThumbnail)
      throws ProcessingFailedException {
    var maxImageSize = getMaxImageSize(isThumbnail);
    var dimension = getDimensions(originalImage, maxImageSize);
    var resizedImage = downsizeImage(dimension, originalImage);
    s3Repository.uploadResults(resizedImage, media.getId(), isThumbnail);
    log.info(
        "Finished uploading {} of image with id {}", isThumbnail ? "thumbnail" : "derivative",
        media.getId());
    return resizedImage;
  }

  private float getMaxImageSize(boolean isThumbnail) {
    return isThumbnail ? properties.getMaxThumbnailImageSize()
        : properties.getMaxDerivativeImageSize();
  }

  private void publishDigitalMedia(DigitalMedia media) throws ProcessingFailedException {
    try {
      var wrapper = new DigitalMediaWrapper(media.getType(), media, null);
      var event = new DigitalMediaEvent(Collections.emptySet(), wrapper, false, false);
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

  private void setMediaDerivative(DigitalMedia media, BufferedImage resizedImage,
      boolean isThumbnail) {
    var now = Date.from(Instant.now());
    var imageType = isThumbnail ? "Thumbnail" : "Derivative";
    var maxImageSize = getMaxImageSize(isThumbnail);
    var derivative = new DigitalMediaDerivative()
        .withAcAccessURI(generateAccessURI(media, isThumbnail))
        .withDctermsTitle(imageType + " of " + media.getId())
        .withDctermsDescription(
            "Image " + imageType
                + " created by DiSSCo after creation of the Digital Media, maximum size of "
                + maxImageSize + " pixels on the longest side.")
        .withExifPixelXDimension(resizedImage.getWidth())
        .withExifPixelYDimension(resizedImage.getHeight())
        .withDctermsCreated(now)
        .withOdsHasAgents(List.of(AgentUtils.createMachineAgent(properties.getName(),
            properties.getPid(), "media-derivative-service", DctermsType.DOI,
            Type.SCHEMA_SOFTWARE_APPLICATION)))
        .withDctermsFormat("image/jpeg")
        .withDctermsModified(now)
        .withDctermsRights(media.getDctermsRights())
        .withAcSubtype(media.getAcSubtype())
        .withAcSubjectOrientation(media.getAcSubjectOrientation())
        .withAcSubjectOrientationLiteral(media.getAcSubjectOrientationLiteral())
        .withAcSubtypeLiteral(media.getAcSubtypeLiteral())
        .withDctermsType(
            DigitalMediaDerivative.DctermsType.fromValue(media.getDctermsType().value()));
    if (media.getOdsHasMediaDerivatives() == null) {
      var derivativeList = new ArrayList<DigitalMediaDerivative>();
      derivativeList.add(derivative);
      media.setOdsHasMediaDerivatives(derivativeList);
    } else {
      media.getOdsHasMediaDerivatives().add(derivative);
    }
  }

  private String generateAccessURI(DigitalMedia media, boolean isThumbnail) {
    var baseUri = properties.getApiUrl() + stripDoiPrefix(media.getId());
    if (isThumbnail) {
      return baseUri + "/thumbnail";
    } else {
      return baseUri + "/derivative";
    }
  }

  private static String stripDoiPrefix(String id) {
    return id.replace(DOI_PROXY, "");
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

  private Pair<Float, Float> getDimensions(BufferedImage originalImage, Float maxImageSize) {
    var width = originalImage.getWidth();
    var height = originalImage.getHeight();
    var longestSide = Math.max(width, height);
    if (longestSide <= maxImageSize) {
      return Pair.of((float) width, (float) height);
    }
    if (width > height) {
      return Pair.of(maxImageSize, height / (width / maxImageSize));
    } else if (height > width) {
      return Pair.of(width / (height / maxImageSize), maxImageSize);
    } else {
      return Pair.of(maxImageSize, maxImageSize);
    }
  }

}
