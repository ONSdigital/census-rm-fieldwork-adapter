package uk.gov.ons.census.fwmtadapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
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
    UriComponents uriComponents = createUriComponents("/cases/{caseId}", caseId);
    return restTemplate.getForObject(uriComponents.toUri().toString(), CaseContainer.class);
  }

  public CaseIdAddressTypeDto getCaseIdAndAddressTypeFromQid(String questionnaire_id) {
    UriComponents uriComponents = createUriComponents("/cases/qid/{qid}", questionnaire_id);
    return restTemplate.getForObject(uriComponents.toUri().toString(), CaseIdAddressTypeDto.class);
  }

  private UriComponents createUriComponents(String path, String id) {
    return UriComponentsBuilder.newInstance()
        .scheme("http")
        .host(host)
        .port(port)
        .path(path)
        .buildAndExpand(id);
  }
}
