package eu.dissco.core.digitalmediaderivativeservice.repository;

import eu.dissco.core.digitalmediaderivativeservice.exception.S3UploadException;
import eu.dissco.core.digitalmediaderivativeservice.property.S3Properties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Slf4j
@Repository
@RequiredArgsConstructor
public class S3Repository {

  private final S3AsyncClient s3Client;
  private final S3Properties properties;

  private static ByteArrayInputStream imageToInputStream(BufferedImage resizedImage)
      throws IOException {
    var baos = new ByteArrayOutputStream();
    ImageIO.write(resizedImage, "jpeg", baos);
    return new ByteArrayInputStream(baos.toByteArray());
  }

  public void uploadResults(BufferedImage image, String doi) throws S3UploadException, IOException {
    log.info("Uploading results to S3");
    var strippedDoi = doi.replace("https://doi.org/TEST/", "");
    var fileName = strippedDoi + '/' + strippedDoi + "-derivative.jpg";
    var inputStream = imageToInputStream(image);
    try {
      s3Client.putObject(request ->
              request
                  .bucket(properties.getBucketName())
                  .key(fileName),
          AsyncRequestBody.fromBytes(inputStream.readAllBytes())).get();
    } catch (ExecutionException e) {
      log.error("Failed to upload image to s3", e);
      throw new S3UploadException();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted when uploading image to s3", e);
      throw new S3UploadException();
    }
  }

}
