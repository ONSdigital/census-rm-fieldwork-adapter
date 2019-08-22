package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;

@Data
public class InvalidAddress {
  private InvalidAddressReason reason;
  private CollectionCase collectionCase;
}
