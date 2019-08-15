package uk.gov.ons.census.fwmtadapter.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.census.fwmtadapter.util.ReceiptHelper.setUpResponseManagementReceiptEvent;

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
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdAddressTypeDto;
import uk.gov.ons.census.fwmtadapter.model.dto.ReceiptDTO;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("nologging")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiptReceiverIT {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private static final String TEST_QID = "test_qid";
  private ObjectMapper objectMapper = new ObjectMapper();

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.receipt-queue}")
  private String receiptQueue;

  @Value("${queueconfig.receipt-routing-key}")
  private String receiptRoutingKey;

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
  }

  @Test
  public void testGoodReceiptMessage()
      throws InterruptedException, JAXBException, JsonProcessingException {
    // Given
    String url = "/cases/qid/?qid=" + TEST_QID;
    CaseIdAddressTypeDto caseIdAddressTypeDto = new CaseIdAddressTypeDto();
    caseIdAddressTypeDto.setCaseId(TEST_CASE_ID);
    caseIdAddressTypeDto.setAddressType(TEST_ADDRESS_TYPE);
    String returnJson = objectMapper.writeValueAsString(caseIdAddressTypeDto);

    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);

    // when
    rabbitQueueHelper.sendMessage(caseEventExchange, receiptRoutingKey, responseManagementEvent);

    // then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }

  @Test
  public void testGoodReceiptMessageWhereCaseLookFailsAtFirstCausingTransactionRollback()
      throws InterruptedException, JsonProcessingException, JAXBException {
    // Given

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);
    String url = "/cases/qid/?qid=" + TEST_QID;

    stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

    // when
    rabbitQueueHelper.sendMessage(caseEventExchange, receiptRoutingKey, responseManagementEvent);

    // then
    rabbitQueueHelper.checkNoMessage(outboundQueue);

    // then again, now add a good stub
    CaseIdAddressTypeDto caseIdAddressTypeDto = new CaseIdAddressTypeDto();
    caseIdAddressTypeDto.setCaseId(TEST_CASE_ID);
    caseIdAddressTypeDto.setAddressType(TEST_ADDRESS_TYPE);

    String returnJson = objectMapper.writeValueAsString(caseIdAddressTypeDto);

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
    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }
}
