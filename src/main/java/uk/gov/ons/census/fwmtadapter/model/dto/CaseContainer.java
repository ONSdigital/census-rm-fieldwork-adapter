package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CaseContainer {

  private String caseRef;

  @JsonProperty("id")
  private String caseId;

  private String arid;

  private String estabArid;

  private String estabType;

  private String uprn;

  @JsonProperty("caseType")
  private String addressType;

  private OffsetDateTime createdDateTime;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String postcode;

  private String organisationName;

  private String addressLevel;

  private String abpCode;

  private String region;

  private String latitude;

  private String longitude;

  private String oa;

  private String lsoa;

  private String msoa;

  private String lad;

  private String state;

  private List<EventDTO> caseEvents;
}
