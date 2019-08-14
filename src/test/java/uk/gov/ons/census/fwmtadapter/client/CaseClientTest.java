package uk.gov.ons.census.fwmtadapter.client;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainer;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdAddressTypeDto;

public class CaseClientTest {
  private static final String QUESTIONAIRE_ID = "123a";
  private static final String CASE_ID = "g5dv3";
  public static final String ADDRESS_TYPE_TEST = "address_type_test";

  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  @Test
  public void successfulyGetCaseIdFromQid() {
    String expectedUrl = "http://" + host + ":" + port + "/cases/qid/" + QUESTIONAIRE_ID;

    CaseIdAddressTypeDto expectedCaseIdAddressTypeDto = new CaseIdAddressTypeDto();
    expectedCaseIdAddressTypeDto.setCaseId(CASE_ID);
    expectedCaseIdAddressTypeDto.setAddressType(ADDRESS_TYPE_TEST);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUrl, CaseIdAddressTypeDto.class))
        .thenReturn(expectedCaseIdAddressTypeDto);

    CaseClient caseClient = new CaseClient(restTemplate);

    assertThat(caseClient.getCaseIdAndAddressTypeFromQid(QUESTIONAIRE_ID))
        .isEqualTo(expectedCaseIdAddressTypeDto);
  }

  @Test
  public void successfullyGetCaseByCaseId() {
    String expectedUrl = "http://" + host + ":" + port + "/cases/" + CASE_ID;
    EasyRandom easyRandom = new EasyRandom();

    CaseContainer caseContainer = easyRandom.nextObject(CaseContainer.class);
    caseContainer.setCaseId(CASE_ID);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUrl, CaseContainer.class)).thenReturn(caseContainer);

    CaseClient caseClient = new CaseClient(restTemplate);

    assertThat(caseClient.getCaseFromCaseId(CASE_ID)).isEqualTo(caseContainer);
  }
}
