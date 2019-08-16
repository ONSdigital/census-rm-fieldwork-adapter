package uk.gov.ons.census.fwmtadapter.client;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;

public class CaseClientTest {
  private static final String QUESTIONAIRE_ID = "123a";
  private static final String CASE_ID = "g5dv3";
  private static final String ADDRESS_TYPE_TEST = "address_type_test";

  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  @Test
  public void successfulyGetCaseIdFromQid() {
    UriComponents expectedUri = createUriComponents("/cases/qid/{qid}", QUESTIONAIRE_ID);

    CaseContainerDto expectedCaseIdAddressTypeDto = new CaseContainerDto();
    expectedCaseIdAddressTypeDto.setCaseId(CASE_ID);
    expectedCaseIdAddressTypeDto.setAddressType(ADDRESS_TYPE_TEST);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUri.toUri().toString(), CaseContainerDto.class))
        .thenReturn(expectedCaseIdAddressTypeDto);

    CaseClient caseClient = new CaseClient(restTemplate);

    assertThat(caseClient.getCaseFromQid(QUESTIONAIRE_ID)).isEqualTo(expectedCaseIdAddressTypeDto);
  }

  @Test
  public void successfullyGetCaseByCaseId() {
    UriComponents expectedUri = createUriComponents("/cases/{caseId}", CASE_ID);
    EasyRandom easyRandom = new EasyRandom();

    CaseContainerDto caseContainerDto = easyRandom.nextObject(CaseContainerDto.class);
    caseContainerDto.setCaseId(CASE_ID);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUri.toUri().toString(), CaseContainerDto.class))
        .thenReturn(caseContainerDto);

    CaseClient caseClient = new CaseClient(restTemplate);

    assertThat(caseClient.getCaseFromCaseId(CASE_ID)).isEqualTo(caseContainerDto);
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
