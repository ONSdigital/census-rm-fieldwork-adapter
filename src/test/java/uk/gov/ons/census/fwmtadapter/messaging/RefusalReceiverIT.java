package uk.gov.ons.census.fwmtadapter.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import org.jeasy.random.EasyRandom;
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
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.Event;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.Refusal;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class RefusalReceiverIT {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private static final String REFUSAL_ROUTING_KEY = "event.respondent.refusal";
  private static final String ADAPTER_OUTBOUND_QUEUE = "RM.Field";

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  private final EasyRandom easyRandom = new EasyRandom();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(refusalQueue);
    rabbitQueueHelper.purgeQueue(ADAPTER_OUTBOUND_QUEUE);
  }

  @Test
  public void testRefusalMessageFromNonFieldChannelEmitsMessageToField()
      throws InterruptedException, JAXBException, JsonProcessingException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);

    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    Refusal refusal = new Refusal();
    refusal.setCollectionCase(collectionCase);
    Payload payload = new Payload();
    payload.setRefusal(refusal);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    Event event = new Event();
    event.setType(EventType.REFUSAL_RECEIVED);
    event.setChannel("CC");
    responseManagementEvent.setEvent(event);

    String url = "/cases/" + TEST_CASE_ID;
    CaseContainerDto caseContainerDto = new CaseContainerDto();
    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
    String returnJson = objectMapper.writeValueAsString(caseContainerDto);

    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    rabbitQueueHelper.sendMessage(caseEventExchange, REFUSAL_ROUTING_KEY, responseManagementEvent);

    // Then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    assertThat(actualMessage).isNotNull();
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);
    assertThat(responseManagementEvent.getPayload().getRefusal().getCollectionCase().getId())
        .isEqualTo(actionInstruction.getActionCancel().getCaseId());
    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }

  @Test
  public void testRefusalMessageFromFieldChannelDoesNotEmitMessageToField()
      throws InterruptedException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);

    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    Refusal refusal = new Refusal();
    refusal.setCollectionCase(collectionCase);
    Payload payload = new Payload();
    payload.setRefusal(refusal);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    Event event = new Event();
    event.setType(EventType.REFUSAL_RECEIVED);
    event.setChannel("FIELD");
    responseManagementEvent.setEvent(event);

    // When
    rabbitQueueHelper.sendMessage(caseEventExchange, REFUSAL_ROUTING_KEY, responseManagementEvent);

    // Then
    assertThat(rabbitQueueHelper.getMessage(outboundQueue)).isNull();
  }
}
