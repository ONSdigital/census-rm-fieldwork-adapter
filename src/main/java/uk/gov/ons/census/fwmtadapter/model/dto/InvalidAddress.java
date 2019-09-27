package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;

@Data
public class InvalidAddress {
  private String reason;
  private CollectionCase collectionCase;
}
