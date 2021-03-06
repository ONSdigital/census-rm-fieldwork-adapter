package uk.gov.ons.census.fwmtadapter.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class FieldworkFollowup {
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String estabType;
  private String organisationName;
  private String uprn;
  private String estabUprn;
  private String oa;
  private String latitude;
  private String longitude;
  private String actionPlan;
  private String actionType;
  private UUID caseId;
  private String caseRef;
  private String addressType;
  private String addressLevel;
  private String treatmentCode;
  private String fieldOfficerId;
  private String fieldCoordinatorId;
  private Integer ceExpectedCapacity;
  private int ceActualResponses;
  private String surveyName;
  private Boolean blankQreReturned;
  private Boolean receipted;
  private Boolean handDelivery;
  private CaseMetadata metadata;
}
