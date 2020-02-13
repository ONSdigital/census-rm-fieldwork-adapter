//package uk.gov.ons.census.fwmtadapter.messaging;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.tomakehurst.wiremock.junit.WireMockRule;
//import org.jeasy.random.EasyRandom;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.transaction.annotation.Transactional;
//import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
//import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
//import uk.gov.ons.census.fwmtadapter.model.dto.Event;
//import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
//import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
//import uk.gov.ons.census.fwmtadapter.model.dto.Refusal;
//import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
//import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
//import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;
//import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;
//
//import java.io.IOException;
//import java.util.concurrent.BlockingQueue;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@ContextConfiguration
//@ActiveProfiles("test")
//@SpringBootTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@RunWith(SpringJUnit4ClassRunner.class)
//public class RefusalReceiverIT {
//  private static final String TEST_CASE_ID = "test_case_id";
//  private static final String TEST_ADDRESS_TYPE = "test_address_type";
//  private static final String REFUSAL_ROUTING_KEY = "event.respondent.refusal";
//  private static final String ADAPTER_OUTBOUND_QUEUE = "RM.Field";
//  private static final String UNIT_ADDRESS_LEVEL = "U";
//  private static final String ESTAB_ADDRESS_LEVEL = "E";
//  private static final String FIELD_CHANNEL = "FIELD";
//
//  @Value("${queueconfig.case-event-exchange}")
//  private String caseEventExchange;
//
//  @Value("${queueconfig.refusal-queue}")
//  private String refusalQueue;
//
//  @Autowired private RabbitQueueHelper rabbitQueueHelper;
//
//  private final EasyRandom easyRandom = new EasyRandom();
//  private final ObjectMapper objectMapper = new ObjectMapper();
//
//  @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));
//
//  @Before
//  @Transactional
//  public void setUp() {
//    rabbitQueueHelper.purgeQueue(refusalQueue);
//    rabbitQueueHelper.purgeQueue(ADAPTER_OUTBOUND_QUEUE);
//  }
//
//  @Test
//  public void testRefusalMessageFromNonFieldChannelEmitsMessageToField()
//      throws InterruptedException, IOException {
//    // Given
//    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);
//
//    CollectionCase collectionCase = new CollectionCase();
//    collectionCase.setId(TEST_CASE_ID);
//    Refusal refusal = new Refusal();
//    refusal.setCollectionCase(collectionCase);
//    Payload payload = new Payload();
//    payload.setRefusal(refusal);
//    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
//    responseManagementEvent.setPayload(payload);
//    Event event = new Event();
//    event.setType(EventType.REFUSAL_RECEIVED);
//    event.setChannel("CC");
//    responseManagementEvent.setEvent(event);
//
//    String url = "/cases/" + TEST_CASE_ID;
//    CaseContainerDto caseContainerDto = new CaseContainerDto();
//    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
//    caseContainerDto.setCaseId(TEST_CASE_ID);
//    caseContainerDto.setAddressLevel(UNIT_ADDRESS_LEVEL);
//    String returnJson = objectMapper.writeValueAsString(caseContainerDto);
//
//    stubFor(
//        get(urlEqualTo(url))
//            .willReturn(
//                aResponse()
//                    .withStatus(HttpStatus.OK.value())
//                    .withHeader("Content-Type", "application/json")
//                    .withBody(returnJson)));
//
//    rabbitQueueHelper.sendMessage(caseEventExchange, REFUSAL_ROUTING_KEY, responseManagementEvent);
//
//    // Then
//    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
//    assertThat(actualMessage).isNotNull();
//
//    ObjectMapper objectMapper = new ObjectMapper();
//    FwmtCloseActionInstruction actionInstruction =
//        objectMapper.readValue(actualMessage, FwmtCloseActionInstruction.class);
//    assertThat(actionInstruction.getActionInstruction()).isEqualTo(ActionInstructionType.CLOSE);
//    assertThat(responseManagementEvent.getPayload().getRefusal().getCollectionCase().getId())
//        .isEqualTo(actionInstruction.getCaseId());
//
//    assertThat(actionInstruction.getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
//  }
//
//  @Test
//  public void testRefusalMessageFromFieldChannelDoesNotEmitMessageToField()
//      throws InterruptedException, JsonProcessingException {
//    // Given
//    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);
//
//    CollectionCase collectionCase = new CollectionCase();
//    collectionCase.setId(TEST_CASE_ID);
//    Refusal refusal = new Refusal();
//    refusal.setCollectionCase(collectionCase);
//    Payload payload = new Payload();
//    payload.setRefusal(refusal);
//    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
//    responseManagementEvent.setPayload(payload);
//    Event event = new Event();
//    event.setType(EventType.REFUSAL_RECEIVED);
//    event.setChannel("FIELD");
//    responseManagementEvent.setEvent(event);
//
//    String url = "/cases/" + TEST_CASE_ID;
//    CaseContainerDto caseContainerDto = new CaseContainerDto();
//    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
//    caseContainerDto.setAddressLevel(ESTAB_ADDRESS_LEVEL);
//    String returnJson = objectMapper.writeValueAsString(caseContainerDto);
//
//    stubFor(
//        get(urlEqualTo(url))
//            .willReturn(
//                aResponse()
//                    .withStatus(HttpStatus.OK.value())
//                    .withHeader("Content-Type", "application/json")
//                    .withBody(returnJson)));
//
//    // When
//    rabbitQueueHelper.sendMessage(caseEventExchange, REFUSAL_ROUTING_KEY, responseManagementEvent);
//
//    // Then
//    assertThat(rabbitQueueHelper.getMessage(outboundQueue)).isNull();
//  }
//}
