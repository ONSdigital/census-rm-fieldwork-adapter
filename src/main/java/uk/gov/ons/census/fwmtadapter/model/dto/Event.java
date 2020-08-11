package uk.gov.ons.census.fwmtadapter.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class Event {
  private EventType type;
  private String source;
  private String channel;
  private OffsetDateTime dateTime;
  private UUID transactionId;
}
