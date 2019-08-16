package uk.gov.ons.census.fwmtadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class EventDTO {

  private String id;

  private String eventType;

  @JsonProperty("description")
  private String eventDescription;

  @JsonProperty("createdDateTime")
  private OffsetDateTime eventDate;
}
