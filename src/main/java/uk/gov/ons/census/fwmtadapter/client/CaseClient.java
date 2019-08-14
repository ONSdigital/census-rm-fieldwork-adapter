package uk.gov.ons.census.fwmtadapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainer;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdAddressTypeDto;

@Component
public class CaseClient {
  private final RestTemplate restTemplate;

  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  public CaseClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public CaseContainer getCaseFromCaseId(String caseId) {
    String url = "http://" + host + ":" + port + "/" + caseId;
    return restTemplate.getForObject(url, CaseContainer.class);
  }

  public CaseIdAddressTypeDto getCaseIdAndAddressTypeFromQid(String questionnaire_id) {
    String url = "http://" + host + ":" + port + "/cases/qid/" + questionnaire_id;
    return restTemplate.getForObject(url, CaseIdAddressTypeDto.class);
  }
}
