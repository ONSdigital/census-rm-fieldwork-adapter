package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;

@Data
public class Payload {
  private CollectionCase collectionCase;
  private Metadata metadata;
}
