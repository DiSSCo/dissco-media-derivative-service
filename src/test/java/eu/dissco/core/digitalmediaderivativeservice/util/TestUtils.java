package eu.dissco.core.digitalmediaderivativeservice.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaEvent;
import eu.dissco.core.digitalmediaderivativeservice.domain.DigitalMediaWrapper;
import eu.dissco.core.digitalmediaderivativeservice.schema.Agent.Type;
import eu.dissco.core.digitalmediaderivativeservice.schema.CreateUpdateTombstoneEvent;
import eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMedia;
import eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMediaDerivative.DctermsType;
import eu.dissco.core.digitalmediaderivativeservice.schema.Identifier;
import eu.dissco.core.digitalmediaderivativeservice.utils.AgentUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestUtils {

  public static final String PREFIX = "Test";
  public static final Instant CREATED = Instant.parse("2025-12-10T16:29:24.000Z");
  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
      .setSerializationInclusion(Include.NON_NULL);

  public static DigitalMediaEvent givenDigitalMediaEvent() throws JsonProcessingException {
    return new DigitalMediaEvent(Collections.emptySet(),
        new DigitalMediaWrapper("ods:DigitalMedia", givenDigitalMedia(), null), false);
  }

  public static DigitalMediaEvent givenDigitalMediaWithDerivativeEvent(int originalWidth,
      int originalHeight, int width, int height)
      throws JsonProcessingException {
    return new DigitalMediaEvent(Collections.emptySet(),
        new DigitalMediaWrapper("ods:DigitalMedia",
            givenDigitalMediaWithDerivative(originalWidth, originalHeight, width, height),
            null), false);
  }

  public static DigitalMedia givenDigitalMediaWithDerivative(int originalWidth, int originalHeight,
      int width, int height)
      throws JsonProcessingException {
    var digitalMedia = givenDigitalMedia();
    digitalMedia.setExifPixelXDimension(originalWidth);
    digitalMedia.setExifPixelYDimension(originalHeight);
    var derivative = new eu.dissco.core.digitalmediaderivativeservice.schema.DigitalMediaDerivative()
        .withDctermsTitle("Derivative of https://doi.org/TEST/WKT-SQB-ZNC")
        .withDctermsDescription(
            "Image Derivative created by DiSSCo after creation of the Digital Media, maximum size of 2048.0 pixels on the longest side.")
        .withDctermsModified(Date.from(CREATED))
        .withDctermsCreated(Date.from(CREATED))
        .withDctermsRights("http://creativecommons.org/publicdomain/zero/1.0/legalcode")
        .withDctermsType(DctermsType.STILL_IMAGE)
        .withDctermsFormat("image/jpeg")
        .withAcAccessURI(
            "https://dev.dissco.tech/api/dm/v1/TEST/WKT-SQB-ZNC/derivative")
        .withExifPixelXDimension(width)
        .withExifPixelYDimension(height)
        .withOdsHasAgents(List.of(AgentUtils.createMachineAgent("DiSSCo Media Derivative Service",
            "https://doi.org/10.5281/zenodo.17935570", "media-derivative-service",
            Identifier.DctermsType.DOI,
            Type.SCHEMA_SOFTWARE_APPLICATION)));
    digitalMedia.setOdsHasMediaDerivatives(Collections.singletonList(derivative));
    return digitalMedia;
  }


  public static CreateUpdateTombstoneEvent getCreateUpdateTombstoneEvent()
      throws JsonProcessingException {
    return MAPPER.convertValue(givenProvenanceEventJson(), CreateUpdateTombstoneEvent.class);
  }

  public static DigitalMedia givenDigitalMedia() throws JsonProcessingException {
    return MAPPER.readValue(givenDigitalMediaString(), DigitalMedia.class);
  }

  public static String givenDigitalMediaString() {
    return """
        {
                  "@id": "https://doi.org/TEST/WKT-SQB-ZNC",
                  "@type": "ods:DigitalMedia",
                  "dcterms:identifier": "https://doi.org/TEST/WKT-SQB-ZNC",
                  "ods:fdoType": "https://doi.org/21.T11148/bbad8c4e101e8af01115",
                  "ods:version": 1,
                  "ods:status": "Active",
                  "dcterms:modified": "2025-01-28T08:49:43.426Z",
                  "dcterms:created": "2025-01-28T11:29:05.116Z",
                  "dcterms:type": "StillImage",
                  "ac:accessURI": "https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large",
                  "ods:sourceSystemID": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                  "ods:sourceSystemName": "Naturalis Biodiversity Center (NL) - Lepidoptera",
                  "ods:organisationID": "https://ror.org/0566bfb96",
                  "ods:organisationName": "Naturalis Biodiversity Center",
                  "dcterms:format": "image/jpeg",
                  "dcterms:rights": "http://creativecommons.org/publicdomain/zero/1.0/legalcode",
                  "ac:variant": "ac:GoodQuality",
                  "ods:hasIdentifiers": [
                    {
                      "@id": "RMNH.INS.1339663@CRS",
                      "@type": "ods:Identifier",
                      "dcterms:title": "dwca:ID",
                      "dcterms:type": "Locally unique identifier",
                      "dcterms:identifier": "RMNH.INS.1339663@CRS",
                      "ods:gupriLevel": "LocallyUniqueStable"
                    },
                    {
                      "@id": "https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large",
                      "@type": "ods:Identifier",
                      "dcterms:title": "dcterms:identifier",
                      "dcterms:type": "URL",
                      "dcterms:identifier": "https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large",
                      "ods:gupriLevel": "GloballyUniqueStablePersistentResolvable"
                    }
                  ],
                  "ods:hasEntityRelationships": [
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasURL",
                      "dwc:relatedResourceID": "https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large",
                      "ods:relatedResourceURI": "https://medialib.naturalis.nl/file/id/RMNH.INS.1339663_1/format/large",
                      "dwc:relationshipEstablishedDate": "2025-01-28T08:49:43.426Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14379776",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14379776",
                          "schema:name": "DiSSCo Translator Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "data-translator"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14379776",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14379776",
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasOrganisationID",
                      "dwc:relatedResourceID": "https://ror.org/0566bfb96",
                      "ods:relatedResourceURI": "https://ror.org/0566bfb96",
                      "dwc:relationshipEstablishedDate": "2025-01-28T08:49:43.426Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14379776",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14379776",
                          "schema:name": "DiSSCo Translator Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "data-translator"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14379776",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14379776",
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasFDOType",
                      "dwc:relatedResourceID": "https://doi.org/21.T11148/bbad8c4e101e8af01115",
                      "ods:relatedResourceURI": "https://doi.org/21.T11148/bbad8c4e101e8af01115",
                      "dwc:relationshipEstablishedDate": "2025-01-28T08:49:43.426Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14379776",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14379776",
                          "schema:name": "DiSSCo Translator Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "data-translator"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14379776",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14379776",
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasSourceSystemID",
                      "dwc:relatedResourceID": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                      "ods:relatedResourceURI": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                      "dwc:relationshipEstablishedDate": "2025-01-28T08:49:43.426Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14379776",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14379776",
                          "schema:name": "DiSSCo Translator Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "data-translator"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14379776",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14379776",
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasLicense",
                      "dwc:relatedResourceID": "http://creativecommons.org/publicdomain/zero/1.0/legalcode",
                      "ods:relatedResourceURI": "http://creativecommons.org/publicdomain/zero/1.0/legalcode",
                      "dwc:relationshipEstablishedDate": "2025-01-28T08:49:43.426Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14379776",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14379776",
                          "schema:name": "DiSSCo Translator Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "data-translator"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14379776",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14379776",
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "@type": "ods:EntityRelationship",
                      "dwc:relationshipOfResource": "hasDigitalSpecimen",
                      "dwc:relatedResourceID": "https://doi.org/TEST/YPB-M6K-XTZ",
                      "ods:relatedResourceURI": "https://doi.org/TEST/YPB-M6K-XTZ",
                      "dwc:relationshipEstablishedDate": "2025-01-28T11:29:02.140Z",
                      "ods:hasAgents": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14383054",
                          "@type": "schema:SoftwareApplication",
                          "schema:identifier": "https://doi.org/10.5281/zenodo.14383054",
                          "schema:name": "DiSSCo Digital Specimen Processing Service",
                          "ods:hasRoles": [
                            {
                              "@type": "schema:Role",
                              "schema:roleName": "processing-service"
                            }
                          ],
                          "ods:hasIdentifiers": [
                            {
                              "@id": "https://doi.org/10.5281/zenodo.14383054",
                              "@type": "ods:Identifier",
                              "dcterms:title": "DOI",
                              "dcterms:type": "DOI",
                              "dcterms:identifier": "https://doi.org/10.5281/zenodo.14383054",
                              "ods:isPartOfLabel": false,
                              "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                              "ods:identifierStatus": "Preferred"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
        """;
  }

  public static JsonNode givenProvenanceEventJson() throws JsonProcessingException {
    return MAPPER.readTree(
        """
            {
              "_id": "https://doi.org/TEST/WKT-SQB-ZNC/1",
              "@id": "https://doi.org/TEST/WKT-SQB-ZNC/1",
              "@type": "ods:CreateUpdateTombstoneEvent",
              "dcterms:identifier": "https://doi.org/TEST/WKT-SQB-ZNC/1",
              "ods:fdoType": "https://doi.org/21.T11148/d7570227982f70256af3",
              "prov:Activity": {
                "@id": "6b651ddc-2e18-4141-bc31-8d0481ba4019",
                "@type": "ods:Create",
                "prov:wasAssociatedWith": [
                  {
                    "@id": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                    "prov:hadRole": "Requestor"
                  },
                  {
                    "@id": "https://doi.org/10.5281/zenodo.14383386",
                    "prov:hadRole": "Approver"
                  },
                  {
                    "@id": "https://doi.org/10.5281/zenodo.14383386",
                    "prov:hadRole": "Generator"
                  }
                ],
                "prov:endedAtTime": "2025-01-28T11:29:05.317Z",
                "prov:used": "https://doi.org/TEST/WKT-SQB-ZNC/1",
                "rdfs:comment": "Digital Media newly created"
              },
              "prov:Entity": {
                "@id": "https://doi.org/TEST/WKT-SQB-ZNC/1",
                "@type": "ods:DigitalMedia",
                "prov:value": 
            """
            + givenDigitalMediaString() +
            """
                    ,
                    "prov:wasGeneratedBy": "6b651ddc-2e18-4141-bc31-8d0481ba4019"
                  },
                  "ods:hasAgents": [
                    {
                      "@id": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                      "@type": "prov:SoftwareAgent",
                      "schema:identifier": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                      "schema:name": "Naturalis Biodiversity Center (NL) - Lepidoptera",
                      "ods:hasRoles": [
                        {
                          "@type": "schema:Role",
                          "schema:roleName": "source-system"
                        }
                      ],
                      "ods:hasIdentifiers": [
                        {
                          "@id": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                          "@type": "ods:Identifier",
                          "dcterms:title": "HANDLE",
                          "dcterms:type": "Handle",
                          "dcterms:identifier": "https://hdl.handle.net/TEST/M03-RH1-TX1",
                          "ods:isPartOfLabel": false,
                          "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                          "ods:identifierStatus": "Preferred"
                        }
                      ]
                    },
                    {
                      "@id": "https://doi.org/10.5281/zenodo.14383386",
                      "@type": "prov:SoftwareAgent",
                      "schema:identifier": "https://doi.org/10.5281/zenodo.14383386",
                      "schema:name": "DiSSCo Digital Media Processing Service",
                      "ods:hasRoles": [
                        {
                          "@type": "schema:Role",
                          "schema:roleName": "processing-service"
                        }
                      ],
                      "ods:hasIdentifiers": [
                        {
                          "@id": "https://doi.org/10.5281/zenodo.14383386",
                          "@type": "ods:Identifier",
                          "dcterms:title": "DOI",
                          "dcterms:type": "DOI",
                          "dcterms:identifier": "https://doi.org/10.5281/zenodo.14383386",
                          "ods:isPartOfLabel": false,
                          "ods:gupriLevel": "GloballyUniqueStablePersistentResolvableFDOCompliant",
                          "ods:identifierStatus": "Preferred"
                        }
                      ]
                    }
                  ]
                }
                """
    );
  }
}
