package uk.gov.ons.census.fwmtadapter.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.StringReader;
import java.util.concurrent.BlockingQueue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdDto;
import uk.gov.ons.census.fwmtadapter.model.dto.Receipt;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiptReceiverIT {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_QID = "test_qid";
  private static final String TEST_QID_2 = "test_qid_2";
  private ObjectMapper objectMapper = new ObjectMapper();

  @Value("${queueconfig.receipt-queue}")
  private String receiptQueue;

  @Value("${queueconfig.adapter-outbound-queue}")
  private String actionOutboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089).httpsPort(8443));

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(receiptQueue);
    rabbitQueueHelper.purgeQueue(actionOutboundQueue);
    //    wireMockRule.start();
  }

  @Test
  public void testGoodReceiptMessageWithCaseIdPopulated()
      throws InterruptedException, JAXBException {
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    Receipt receipt = new Receipt();
    receipt.setCaseId(TEST_CASE_ID);

    rabbitQueueHelper.sendMessage(receiptQueue, receipt);

    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
  }

  @Test
  public void testGoodReceiptMessageWithoutCaseIdPopulated()
      throws InterruptedException, JAXBException, JsonProcessingException {
    // Given
    String url = "/cases/qid/" + TEST_QID;
    CaseIdDto caseIdDto = new CaseIdDto();
    caseIdDto.setCaseId(TEST_CASE_ID);
    String returnJson = objectMapper.writeValueAsString(caseIdDto);

    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    Receipt receipt = new Receipt();
    receipt.setQuestionnaire_Id(TEST_QID);

    // when
    rabbitQueueHelper.sendMessage(receiptQueue, receipt);

    // then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
  }

  @Test
  public void testGoodReceiptMessageWithoutCaseIdPopulatedCausingTransactionRollback()
      throws InterruptedException, JsonProcessingException, JAXBException {
    // Given

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    Receipt receipt = new Receipt();
    receipt.setQuestionnaire_Id(TEST_QID_2);
    String url = "/cases/qid/" + TEST_QID_2;

    stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

    // when
    rabbitQueueHelper.sendMessage(receiptQueue, receipt);

    // then
    rabbitQueueHelper.checkNoMessage(outboundQueue);

    // then again, now add a good stub
    CaseIdDto caseIdDto = new CaseIdDto();
    caseIdDto.setCaseId(TEST_CASE_ID);

    String returnJson = objectMapper.writeValueAsString(caseIdDto);

    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
  }
}
