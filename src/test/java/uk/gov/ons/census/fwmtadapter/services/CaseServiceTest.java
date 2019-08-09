package uk.gov.ons.census.fwmtadapter.services;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;

public class CaseServiceTest {
  private static final String QUESTIONAIRE_ID = "123a";
  private static final String CASE_ID = "g5dv3";

  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  @Test
  public void successfulyGetCaseIdFromQid() {
    String expectedUrl = "http://" + host + ":" + port + "/cases/qid/" + QUESTIONAIRE_ID;

    CaseIdDto expectedCaseIdDto = new CaseIdDto();
    expectedCaseIdDto.setCaseId(CASE_ID);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUrl, CaseIdDto.class)).thenReturn(expectedCaseIdDto);

    CaseService caseService = new CaseService(restTemplate);

    assertThat(caseService.getCaseIdFromQid(QUESTIONAIRE_ID)).isEqualTo(CASE_ID);
  }

  @Test(expected = RuntimeException.class)
  public void testNullCaseIdReturned() {
    String expectedUrl = "http://" + host + ":" + port + "/cases/qid/" + QUESTIONAIRE_ID;

    CaseIdDto expectedCaseIdDto = new CaseIdDto();

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(expectedUrl, CaseIdDto.class)).thenReturn(expectedCaseIdDto);

    CaseService caseService = new CaseService(restTemplate);

    caseService.getCaseIdFromQid(QUESTIONAIRE_ID);
  }
}
