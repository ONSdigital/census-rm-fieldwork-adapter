package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;

@Data
public class Refusal {

  private RefusalType type;

  private String report;

  private String agentId;

  private CollectionCase collectionCase;
}
