package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
public class Payload {
  @JsonInclude(Include.NON_NULL)
  private Refusal refusal;

  @JsonInclude(Include.NON_NULL)
  private InvalidAddress invalidAddress;

  private Uac uac;
}
