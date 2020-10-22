package uk.gov.ons.census.fwmtadapter.model.dto.fwmt;

import java.util.UUID;
import lombok.Data;

@Data
public class FwmtCancelActionInstruction {
  private ActionInstructionType actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private UUID caseId;
  private Integer ceExpectedCapacity;
  private int ceActualResponses;
}
