package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;

@Service
public class CaseService {
  @Value("${case-api.host}")
  private String host;

  @Value("${case-api.port}")
  private String port;

  public String getCaseIdFromQid(String questionnaire_id) {
    String url = "http://" + host + ":" + port + "/cases/qid/" + questionnaire_id;
    RestTemplate restTemplate = new RestTemplate();

    try {
      CaseIdDto caseIdDto = restTemplate.getForObject(url, CaseIdDto.class);
      return caseIdDto.getCaseId();
    } catch (HttpClientErrorException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
