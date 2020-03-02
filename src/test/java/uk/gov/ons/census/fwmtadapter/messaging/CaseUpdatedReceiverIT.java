package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.Address;
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.Metadata;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseUpdatedReceiverIT {
  private static final String CASE_UPDATE_ROUTING_KEY = "event.case.update";
  private static final String ADAPTER_OUTBOUND_QUEUE = "RM.Field";
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_CASE_REF = "test_case_ref";
  private static final String TEST_SURVEY = "test_survey";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private static final String TEST_ADDRESS_LEVEL = "test_address_level";
  private static final String TEST_ADDRESS_LINE1 = "test_address_line_1";
  private static final String TEST_ADDRESS_POST_TOWN = "test_address_post_town";
  private static final String TEST_ADDRESS_POSTCODE = "test_address_postcode";

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.case-updated-queue}")
  private String caseUpdatedQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(caseUpdatedQueue);
    rabbitQueueHelper.purgeQueue(ADAPTER_OUTBOUND_QUEUE);
  }

  @Test
  public void testGoodCloseReceiptMessage() throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setSurvey(TEST_SURVEY);
    Address address = new Address();
    address.setAddressLevel(TEST_ADDRESS_LEVEL);
    address.setAddressType(TEST_ADDRESS_TYPE);
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CLOSE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // when
    rabbitQueueHelper.sendMessage(
        caseEventExchange, CASE_UPDATE_ROUTING_KEY, responseManagementEvent);

    // then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    ObjectMapper objectMapper = new ObjectMapper();
    FwmtCloseActionInstruction actionInstruction =
        objectMapper.readValue(actualMessage, FwmtCloseActionInstruction.class);
    assertThat(actionInstruction.getActionInstruction()).isEqualTo(ActionInstructionType.CLOSE);
    assertThat(actionInstruction.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getSurveyName()).isEqualTo(TEST_SURVEY);
    assertThat(actionInstruction.getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
    assertThat(actionInstruction.getAddressLevel()).isEqualTo(TEST_ADDRESS_LEVEL);
  }

  @Test
  public void testGoodNewCCSCaseMessage() throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef(TEST_CASE_REF);
    collectionCase.setUndeliveredAsAddressed(Boolean.FALSE);
    Address address = new Address();
    address.setAddressLevel(TEST_ADDRESS_LEVEL);
    address.setAddressType(TEST_ADDRESS_TYPE);
    address.setAddressLine1(TEST_ADDRESS_LINE1);
    address.setTownName(TEST_ADDRESS_POST_TOWN);
    address.setPostcode(TEST_ADDRESS_POSTCODE);
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CREATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // when
    rabbitQueueHelper.sendMessage(
        caseEventExchange, CASE_UPDATE_ROUTING_KEY, responseManagementEvent);

    // then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    ObjectMapper objectMapper = new ObjectMapper();
    FwmtActionInstruction actionInstruction =
        objectMapper.readValue(actualMessage, FwmtActionInstruction.class);
    assertThat(actionInstruction.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actionInstruction.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getCaseRef()).isEqualTo(TEST_CASE_REF);
    assertThat(actionInstruction.getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
    assertThat(actionInstruction.getAddressLevel()).isEqualTo(TEST_ADDRESS_LEVEL);
    assertThat(actionInstruction.getAddressLine1()).isEqualTo(TEST_ADDRESS_LINE1);
    assertThat(actionInstruction.getTownName()).isEqualTo(TEST_ADDRESS_POST_TOWN);
    assertThat(actionInstruction.getPostcode()).isEqualTo(TEST_ADDRESS_POSTCODE);
  }
}
