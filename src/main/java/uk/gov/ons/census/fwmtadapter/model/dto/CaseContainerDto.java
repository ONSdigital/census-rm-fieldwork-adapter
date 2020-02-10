package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CaseContainerDto {
  private String caseRef;

  @JsonProperty("id")
  private String caseId;

  @JsonProperty("caseType")
  private String addressType;

  private String addressLevel;
}
