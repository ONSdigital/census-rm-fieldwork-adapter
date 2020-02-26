package uk.gov.ons.census.fwmtadapter.model.dto.fwmt;

import lombok.Data;

@Data
public class FwmtCloseActionInstruction {
  private ActionInstructionType actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private String caseId;
}
