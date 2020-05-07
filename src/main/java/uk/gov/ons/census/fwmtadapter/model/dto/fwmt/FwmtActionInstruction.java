package uk.gov.ons.census.fwmtadapter.model.dto.fwmt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class FwmtActionInstruction {
  private ActionInstructionType actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private String caseId;
  private String caseRef;
  private String estabType;
  private String fieldOfficerId;
  private String fieldCoordinatorId;
  private String organisationName;
  private String uprn;
  private String estabUprn;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String oa;
  private Double latitude;
  private Double longitude;
  private boolean ce1Complete;
  private boolean handDeliver;
  private Integer ceExpectedCapacity;
  private Integer ceActualResponses;
  private Boolean undeliveredAsAddress;
  private Boolean blankFormReturned;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean secureEstablishment;
}
