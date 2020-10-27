package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class CollectionCase {
  private UUID id;
  private String caseRef;
  private String caseType;
  private String survey;
  private UUID collectionExerciseId;
  private Address address;
  private OffsetDateTime actionableFrom;
  private Boolean receiptReceived;
  private String refusalReceived;
  private OffsetDateTime createdDateTime;
  private OffsetDateTime lastUpdated;

  // Below this line is extra data potentially needed by Action Scheduler - can be ignored by RH
  private UUID actionPlanId;
  private String treatmentCode;
  private String oa;
  private String lsoa;
  private String msoa;
  private String lad;
  private String htcWillingness;
  private String htcDigital;
  private String fieldCoordinatorId;
  private String fieldOfficerId;
  private Integer ceExpectedCapacity;
  private int ceActualResponses;
  private Boolean addressInvalid;
  private boolean handDelivery;
  private CaseMetadata metadata;
}
