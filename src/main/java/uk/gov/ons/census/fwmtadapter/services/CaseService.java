package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;

@Service
public class CaseService {
  public String getCaseIdFromQid(String questionnaire_id) {
    String url = "http://caseapi:8161/cases/qid/" + questionnaire_id;
    RestTemplate restTemplate = new RestTemplate();

    System.out.println("Requesting from url: " + url);
    CaseIdDto caseIdDto = restTemplate.getForObject(url, CaseIdDto.class);
    System.out.println("Got caseIdDto with");
    System.out.println("Case Id: " + caseIdDto.getCaseId());

    return caseIdDto.getCaseId();
  }
}
