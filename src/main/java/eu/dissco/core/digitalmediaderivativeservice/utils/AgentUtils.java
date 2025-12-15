package eu.dissco.core.digitalmediaderivativeservice.utils;

import static eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.DctermsType.DOI;
import static eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.DctermsType.HANDLE;
import static eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT;
import static eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.OdsIdentifierStatus.PREFERRED;

import eu.dissco.core.digitalmediaderivativeservice.schema.Agent;
import eu.dissco.core.digitalmediaderivativeservice.schema.Agent.Type;
import eu.dissco.core.digitalmediaderivativeservice.schema.Identifier;
import eu.dissco.core.digitalmediaderivativeservice.schema.Identifier.DctermsType;
import eu.dissco.core.digitalmediaderivativeservice.schema.OdsHasRole;
import java.util.List;

public class AgentUtils {

  private AgentUtils() {
  }

  public static Agent createMachineAgent(String name, String pid, String role,
      DctermsType idType, Type agentType) {
    var agent = new Agent()
        .withType(agentType)
        .withId(pid)
        .withSchemaName(name)
        .withSchemaIdentifier(pid)
        .withOdsHasRoles(List.of(new OdsHasRole().withType("schema:Role")
            .withSchemaRoleName(role)));
    if (pid != null) {
      var identifier = new Identifier()
          .withType("ods:Identifier")
          .withId(pid)
          .withDctermsIdentifier(pid)
          .withOdsIsPartOfLabel(false)
          .withOdsIdentifierStatus(PREFERRED)
          .withOdsGupriLevel(
              GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT);
      if (idType == DOI) {
        identifier.withDctermsType(DOI);
        identifier.withDctermsTitle("DOI");
      } else if (idType == HANDLE) {
        identifier.withDctermsType(HANDLE);
        identifier.withDctermsTitle("HANDLE");
      }
      agent.setOdsHasIdentifiers(List.of(identifier));
    }
    return agent;
  }
}
