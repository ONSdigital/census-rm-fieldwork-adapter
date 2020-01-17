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
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.Event;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.Uac;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class UacUpdatedReceiverIT {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private static final String TEST_QID = "1120000000000100";
  private static final String UAC_UPDATE_ROUTING_KEY = "event.uac.update";
  private static final String ADAPTER_OUTBOUND_QUEUE = "RM.Field";
  private ObjectMapper objectMapper = new ObjectMapper();

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.uac-updated-queue}")
  private String uacUpdatedQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089).httpsPort(8443));

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(uacUpdatedQueue);
    rabbitQueueHelper.purgeQueue(ADAPTER_OUTBOUND_QUEUE);
  }

  @Test
  public void testGoodReceiptMessage()
      throws InterruptedException, JAXBException, JsonProcessingException {
    // Given
    String url = "/cases/" + TEST_CASE_ID;
    CaseContainerDto caseContainerDto = new CaseContainerDto();
    caseContainerDto.setCaseId(TEST_CASE_ID);
    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
    String returnJson = objectMapper.writeValueAsString(caseContainerDto);

    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);
    Uac uac = new Uac();
    uac.setActive(false);
    uac.setCaseId(TEST_CASE_ID);
    uac.setQuestionnaireId(TEST_QID);
    Payload payload = new Payload();
    payload.setUac(uac);
    Event event = new Event();
    event.setTransactionId("test transaction id");
    event.setChannel("test channel");
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    responseManagementEvent.setEvent(event);

    // when
    rabbitQueueHelper.sendMessage(
        caseEventExchange, UAC_UPDATE_ROUTING_KEY, responseManagementEvent);

    // then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);

    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }
}
