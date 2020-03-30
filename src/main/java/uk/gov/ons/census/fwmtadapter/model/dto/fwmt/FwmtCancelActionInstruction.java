package uk.gov.ons.census.fwmtadapter.model.dto.fwmt;

import lombok.Data;

@Data
public class FwmtCancelActionInstruction {
  private ActionInstructionType actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private String caseId;
  private Integer ceExpectedCapacity;
  private Integer ceActualResponses;
}
