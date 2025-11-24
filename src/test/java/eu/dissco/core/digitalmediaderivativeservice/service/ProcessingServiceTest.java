package eu.dissco.core.digitalmediaderivativeservice.service;

import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.MAPPER;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.getCreateUpdateTombstoneEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.property.ApplicationProperties;
import eu.dissco.core.digitalmediaderivativeservice.repository.S3Repository;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvEntity;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvValue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

  @Mock
  private S3Repository s3Repository;
  @Captor
  private ArgumentCaptor<BufferedImage> imageCaptor;

  private ProcessingService processingService;

  static Stream<Arguments> handleMessageProvider() {
    return Stream.of(
        Arguments.of("test-image-1.jpeg", 1920, 1795),
        Arguments.of("test-image-2.jpeg", 1087, 1700),
        Arguments.of("test-image-3.jpeg", 1000, 1000),
        Arguments.of("test-image-4.jpeg", 2048, 2048),
        Arguments.of("test-image-5.jpeg", 1985, 2048),
        Arguments.of("test-image-6.jpeg", 2048, 1366)
    );
  }

  @BeforeEach
  void setUp() {
    this.processingService = new ProcessingService(MAPPER, new ApplicationProperties(),
        s3Repository);
  }

  @MethodSource("handleMessageProvider")
  @ParameterizedTest
  void testHandleMessage(String testFileName, int width, int height)
      throws ProcessingFailedException, IOException {
    // Given
    var event = getCreateUpdateTombstoneEvent();
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/" + testFileName).getPath()));
    try (MockedStatic<ImageIO> utilities = Mockito.mockStatic(ImageIO.class)) {
      utilities.when(() -> ImageIO.read(
          URI.create("https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large")
              .toURL())).thenReturn(image);

      // When
      processingService.handleMessage(event);

      // Then
      then(s3Repository).should()
          .uploadResults(imageCaptor.capture(), eq("https://doi.org/TEST/WKT-SQB-ZNC"));
      assertThat(imageCaptor.getValue().getWidth()).isEqualTo(width);
      assertThat(imageCaptor.getValue().getHeight()).isEqualTo(height);
    }
  }

  @Test
  void testInvalidDigitalMediaObject() {
    // Given
    var event = new CreateUpdateTombstoneEvent().withProvEntity(
        new ProvEntity().withType("ods:DigitalSpecimen").withProvValue(
            new ProvValue().withAdditionalProperty("ods:someRandomProperty", "someRandomValue")));

    // When / Then
    assertThrows(ProcessingFailedException.class, () -> processingService.handleMessage(event));
  }
}
