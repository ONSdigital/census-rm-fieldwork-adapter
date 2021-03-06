package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.fwmtadapter.model.dto.*;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCancelActionInstruction;

@RunWith(MockitoJUnitRunner.class)
public class CaseReceiverTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @InjectMocks private CaseReceiver underTest;

  private UUID TEST_CASE_ID = UUID.randomUUID();

  @Test
  public void testReceiveCancelDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setSurvey("CENSUS");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CANCEL);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtCancelActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtCancelActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtCancelActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualAi.getSurveyName()).isEqualTo("CENSUS");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CANCEL);
  }

  @Test
  public void testReceiveCreateDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("HH");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    address.setUprn("U1");
    address.setEstabUprn("EstabU2");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CREATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualActionInstruction = aiArgumentCaptor.getValue();
    assertThat(actualActionInstruction.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualActionInstruction.getCaseRef()).isEqualTo("testRef");
    assertThat(actualActionInstruction.getAddressType()).isEqualTo("test address type");
    assertThat(actualActionInstruction.getAddressLevel()).isEqualTo("U");
    assertThat(actualActionInstruction.getActionInstruction())
        .isEqualTo(ActionInstructionType.CREATE);
    assertThat(actualActionInstruction.isSecureEstablishment()).isFalse();
    assertThat(actualActionInstruction.getBlankFormReturned()).isNull();
    assertThat(actualActionInstruction.getUprn()).isEqualTo("U1");
    assertThat(actualActionInstruction.getEstabUprn()).isEqualTo("EstabU2");
    assertThat(actualActionInstruction.getUndeliveredAsAddress()).isNull();
  }

  @Test
  public void testReceiveCreateDecisionCE() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("CE");

    CaseMetadata ceMetadata = new CaseMetadata();
    ceMetadata.setSecureEstablishment(true);
    collectionCase.setMetadata(ceMetadata);
    Address address = new Address();
    address.setAddressLevel("E");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CREATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("E");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actualAi.isSecureEstablishment()).isTrue();
    assertThat(actualAi.getBlankFormReturned()).isNull();
  }

  @Test
  public void testReceiveUpdateDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("HH");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.UPDATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
    assertThat(actualAi.getBlankFormReturned()).isNull();
  }

  @Test
  public void testReceiveUpdateDecisionCE() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("CE");

    CaseMetadata ceMetadata = new CaseMetadata();
    ceMetadata.setSecureEstablishment(false);
    collectionCase.setMetadata(ceMetadata);
    Address address = new Address();
    address.setAddressLevel("E");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.UPDATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("E");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
    assertThat(actualAi.isSecureEstablishment()).isFalse();
    assertThat(actualAi.getBlankFormReturned()).isNull();
  }

  @Test
  public void testReceiveUpdateDecisionSPG() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("SPG");

    CaseMetadata ceMetadata = new CaseMetadata();
    ceMetadata.setSecureEstablishment(true);
    collectionCase.setMetadata(ceMetadata);
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.UPDATE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
    assertThat(actualAi.isSecureEstablishment()).isTrue();
    assertThat(actualAi.getBlankFormReturned()).isNull();
  }

  @Test
  public void testReceiveCreateDecisionWtihClericalAddressResolution() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("HH");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    address.setUprn("U1");
    address.setEstabUprn("EstabU2");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CREATE);
    metadata.setCauseEventType(EventType.CLERICAL_ADDRESS_RESOLUTION);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualActionInstruction = aiArgumentCaptor.getValue();
    assertThat(actualActionInstruction.getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualActionInstruction.getCaseRef()).isEqualTo("testRef");
    assertThat(actualActionInstruction.getAddressType()).isEqualTo("test address type");
    assertThat(actualActionInstruction.getAddressLevel()).isEqualTo("U");
    assertThat(actualActionInstruction.getActionInstruction())
        .isEqualTo(ActionInstructionType.CREATE);
    assertThat(actualActionInstruction.isSecureEstablishment()).isFalse();
    assertThat(actualActionInstruction.getBlankFormReturned()).isNull();
    assertThat(actualActionInstruction.getUprn()).isEqualTo("U1");
    assertThat(actualActionInstruction.getEstabUprn()).isEqualTo("EstabU2");
    assertThat(actualActionInstruction.getUndeliveredAsAddress()).isTrue();
  }

  @Test
  public void testReceiveUpdateDecisionWithBlankQuestionnaireReturned() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(TEST_CASE_ID);
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("HH");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.UPDATE);

    // Note blank questionnaire set to true in the metadata
    metadata.setBlankQuestionnaireReceived(true);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtActionInstruction actualAi = aiArgumentCaptor.getValue();

    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
    assertThat(actualAi.getBlankFormReturned()).isTrue();
  }

  @Test
  public void testIgnoresEventWithNoMetadata() {
    // Given
    CollectionCase collectionCase = new CollectionCase();

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test(expected = RuntimeException.class)
  public void testErrorsOnUnimplementedFieldDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.PAUSE);
    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When, then throws
    underTest.receiveMessage(responseManagementEvent);
  }
}
