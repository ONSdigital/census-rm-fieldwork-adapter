package uk.gov.ons.census.fwmtadapter.model.dto;

import lombok.Data;

@Data
public class Uac {
  private String uacHash;
  private String uac;
  private boolean active;
  private String questionnaireId;
  private String caseType;
  private String region;
  private String caseId;
  private String collectionExerciseId;
  private boolean unreceipted;
}
