package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;

@Data
public class Metadata {
  private ActionInstructionType fieldDecision;
  private EventType causeEventType;
}
