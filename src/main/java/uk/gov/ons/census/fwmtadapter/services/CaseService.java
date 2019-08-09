package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;

@Service
public class CaseService {
  private final RestTemplate restTemplate;

  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  public CaseService(RestTemplate restTemplate) {
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
