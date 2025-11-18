package eu.dissco.core.digitalmediaderivativeservice.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import eu.dissco.core.digitalmediaderivativeservice.exception.S3UploadException;
import eu.dissco.core.digitalmediaderivativeservice.property.S3Properties;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
class S3RepositoryTest {

  @Mock
  private S3AsyncClient s3AsyncClient;
  @Mock
  private S3Properties s3Properties;

  private S3Repository s3Repository;

  @BeforeEach
  void setUp() {
    this.s3Repository = new S3Repository(s3AsyncClient, s3Properties);
  }

  @Test
  void testUploadResults()
      throws IOException, S3UploadException, ExecutionException, InterruptedException {
    // Given
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/test-image-1.jpeg").getPath()));
    var mockResponse = mock(CompletableFuture.class);
    given(s3AsyncClient.putObject(any(Consumer.class), any(AsyncRequestBody.class))).willReturn(
        mockResponse);

    // When
    s3Repository.uploadResults(image, "https://doi.org/TEST/XXX-XXX-XXX");

    // Then
    then(mockResponse).should().get();
  }

  @Test
  void testUploadResultsExecutionException()
      throws IOException, ExecutionException, InterruptedException {
    // Given
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/test-image-1.jpeg").getPath()));
    var mockResponse = mock(CompletableFuture.class);
    given(s3AsyncClient.putObject(any(Consumer.class), any(AsyncRequestBody.class))).willReturn(
        mockResponse);
    given(mockResponse.get()).willThrow(ExecutionException.class);

    // When  / Then
    assertThrows(S3UploadException.class,
        () -> s3Repository.uploadResults(image, "https://doi.org/TEST/XXX-XXX-XXX"));
  }

  @Test
  void testUploadResultsInterruptedException()
      throws IOException, ExecutionException, InterruptedException {
    // Given
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/test-image-1.jpeg").getPath()));
    var mockResponse = mock(CompletableFuture.class);
    given(s3AsyncClient.putObject(any(Consumer.class), any(AsyncRequestBody.class))).willReturn(
        mockResponse);
    given(mockResponse.get()).willThrow(InterruptedException.class);

    // When  / Then
    assertThrows(S3UploadException.class,
        () -> s3Repository.uploadResults(image, "https://doi.org/TEST/XXX-XXX-XXX"));
  }
}
