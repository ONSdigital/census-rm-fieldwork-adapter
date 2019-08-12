package uk.gov.ons.census.fwmtadapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;

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

  public String getCaseIdFromQid(String questionnaire_id) {
    String url = "http://" + host + ":" + port + "/cases/qid/" + questionnaire_id;

    CaseIdDto caseIdDto = restTemplate.getForObject(url, CaseIdDto.class);

    String caseId = caseIdDto.getCaseId();

    if (StringUtils.isEmpty(caseId)) {
      throw new RuntimeException("Returned empty caseID from case api");
    }

    return caseIdDto.getCaseId();
  }
}
