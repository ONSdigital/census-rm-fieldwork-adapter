package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReceiptDTO {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("caseId")
  private String caseId;

  private String questionnaireId;

  private boolean unreceipt;
}
