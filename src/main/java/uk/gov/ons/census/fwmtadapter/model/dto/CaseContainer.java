package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CaseContainer {
  @JsonProperty("id")
  private String caseId;

  @JsonProperty("caseType")
  private String addressType;
}
