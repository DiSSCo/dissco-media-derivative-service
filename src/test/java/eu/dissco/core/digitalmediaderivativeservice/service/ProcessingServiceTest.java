package eu.dissco.core.digitalmediaderivativeservice.service;

import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.CREATED;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.MAPPER;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.getCreateUpdateTombstoneEvent;
import static eu.dissco.core.digitalmediaderivativeservice.util.TestUtils.givenDigitalMediaWithDerivativeEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaEvent;
import eu.dissco.core.digitalmediaderivativeservice.exception.ProcessingFailedException;
import eu.dissco.core.digitalmediaderivativeservice.property.ApplicationProperties;
import eu.dissco.core.digitalmediaderivativeservice.repository.S3Repository;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvActivity;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvActivity.Type;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvEntity;
import eu.dissco.core.digitalmediaderivativeservice.schema.ProvValue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
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
  @Mock
  private RabbitMqPublisherService rabbitMqPublisherService;
  @Captor
  private ArgumentCaptor<BufferedImage> imageCaptor;
  private MockedStatic<Instant> mockedInstant;
  private MockedStatic<Clock> mockedClock;

  private ProcessingService processingService;

  static Stream<Arguments> handleMessageProvider() throws JsonProcessingException {
    return Stream.of(
        Arguments.of("test-image-1.jpeg", 1920, 1795,
            givenDigitalMediaWithDerivativeEvent(1920, 1795, 1920, 1795)),
        Arguments.of("test-image-2.jpeg", 1087, 1700,
            givenDigitalMediaWithDerivativeEvent(1087, 1700, 1087, 1700)),
        Arguments.of("test-image-3.jpeg", 1000, 1000,
            givenDigitalMediaWithDerivativeEvent(1000, 1000, 1000, 1000)),
        Arguments.of("test-image-4.jpeg", 2048, 2048,
            givenDigitalMediaWithDerivativeEvent(3000, 3000, 2048, 2048)),
        Arguments.of("test-image-5.jpeg", 1985, 2048,
            givenDigitalMediaWithDerivativeEvent(3033, 3129, 1985, 2048)),
        Arguments.of("test-image-6.jpeg", 2048, 1366,
            givenDigitalMediaWithDerivativeEvent(7360, 4912, 2048, 1366))
    );
  }

  static Stream<Arguments> invalidMessageProvider() {
    return Stream.of(
        Arguments.of(
            new CreateUpdateTombstoneEvent().withProvActivity(
                new ProvActivity().withType(Type.ODS_CREATE)).withProvEntity(
                new ProvEntity().withType("ods:DigitalSpecimen").withProvValue(
                    new ProvValue().withAdditionalProperty("ods:someRandomProperty",
                        "someRandomValue"))))
    );
  }

  static Stream<Arguments> ignoredMessageProvider() {
    return Stream.of(
        Arguments.of(
            new CreateUpdateTombstoneEvent().withProvActivity(
                new ProvActivity().withType(Type.ODS_TOMBSTONE)).withProvEntity(
                new ProvEntity().withType("ods:DigitalSpecimen").withProvValue(
                    new ProvValue().withAdditionalProperty("ods:someRandomProperty",
                        "someRandomValue")))
        ),
        Arguments.of(
            new CreateUpdateTombstoneEvent().withProvActivity(
                new ProvActivity().withType(Type.ODS_CREATE)).withProvEntity(
                new ProvEntity().withType("ods:DigitalMedia").withProvValue(
                    new ProvValue().withAdditionalProperty("dcterms:format",
                        "application/json")))),
        Arguments.of(
            new CreateUpdateTombstoneEvent().withProvActivity(
                new ProvActivity().withType(Type.ODS_CREATE)).withProvEntity(
                new ProvEntity().withType("ods:DigitalMedia").withProvValue(
                    new ProvValue().withAdditionalProperty("ac:accessURI", "https://an-test-server")
                        .withAdditionalProperty("dcterms:format",
                            "application/json"))))
    );
  }

  @BeforeEach
  void setUp() {
    var properties = new ApplicationProperties();
    properties.setApiUrl("https://dev.dissco.tech/api/dm/v1/");
    this.processingService = new ProcessingService(MAPPER, properties,
        s3Repository, rabbitMqPublisherService);
    Clock clock = Clock.fixed(CREATED, ZoneOffset.UTC);
    Instant instant = Instant.now(clock);
    mockedInstant = mockStatic(Instant.class);
    mockedInstant.when(Instant::now).thenReturn(instant);
    mockedInstant.when(() -> Instant.from(any())).thenReturn(instant);
    mockedInstant.when(() -> Instant.parse(any())).thenReturn(instant);
    mockedClock = mockStatic(Clock.class);
    mockedClock.when(Clock::systemUTC).thenReturn(clock);
  }

  @AfterEach
  void destroy() {
    mockedInstant.close();
    mockedClock.close();
  }

  @MethodSource("handleMessageProvider")
  @ParameterizedTest
  void testHandleMessage(String testFileName, int width, int height,
      DigitalMediaEvent expectedDigitalMediaEvent) throws ProcessingFailedException, IOException {
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
      then(rabbitMqPublisherService).should().publishDigitalMediaEvent(expectedDigitalMediaEvent);
      assertThat(imageCaptor.getValue().getWidth()).isEqualTo(width);
      assertThat(imageCaptor.getValue().getHeight()).isEqualTo(height);
    }
  }

  @MethodSource("invalidMessageProvider")
  @ParameterizedTest
  void testInvalidDigitalMediaObject(CreateUpdateTombstoneEvent event) {
    // Given

    // When / Then
    assertThrows(ProcessingFailedException.class, () -> processingService.handleMessage(event));
  }

  @MethodSource("ignoredMessageProvider")
  @ParameterizedTest
  void testIgnoredDigitalMediaObject(CreateUpdateTombstoneEvent event)
      throws ProcessingFailedException, JsonProcessingException {
    // Given

    // When
    processingService.handleMessage(event);

    // Then
    then(s3Repository).shouldHaveNoInteractions();
    then(rabbitMqPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testEmptyImage() throws IOException {
    // Given
    var event = getCreateUpdateTombstoneEvent();
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/test-iiif.json").getPath()));
    try (MockedStatic<ImageIO> utilities = Mockito.mockStatic(ImageIO.class)) {
      utilities.when(() -> ImageIO.read(
          URI.create("https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large")
              .toURL())).thenReturn(image);

      // When / Then
      assertThrows(ProcessingFailedException.class, () -> processingService.handleMessage(event));
    }
  }

  @Test
  void testIOExceptionImage() throws IOException {
    // Given
    var event = getCreateUpdateTombstoneEvent();
    try (MockedStatic<ImageIO> utilities = Mockito.mockStatic(ImageIO.class)) {
      utilities.when(() -> ImageIO.read(
          URI.create("https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large")
              .toURL())).thenThrow(IOException.class);

      // When / Then
      assertThrows(ProcessingFailedException.class, () -> processingService.handleMessage(event));
    }
  }

  @Test
  void testRabbitExceptionImage() throws IOException, ProcessingFailedException {
    // Given
    var event = getCreateUpdateTombstoneEvent();
    var image = ImageIO.read(new File(
        new ClassPathResource("src/test/resources/test-images/test-image-1.jpeg").getPath()));
    try (MockedStatic<ImageIO> utilities = Mockito.mockStatic(ImageIO.class)) {
      utilities.when(() -> ImageIO.read(
          URI.create("https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large")
              .toURL())).thenReturn(image);
      doThrow(JsonProcessingException.class).when(rabbitMqPublisherService)
          .publishDigitalMediaEvent(any());

      // When
      assertThrows(ProcessingFailedException.class, () -> processingService.handleMessage(event));

      // Then
      then(s3Repository).should()
          .uploadResults(imageCaptor.capture(), eq("https://doi.org/TEST/WKT-SQB-ZNC"));
    }
  }
}
