package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

@RunWith(MockitoJUnitRunner.class)
public class CaseUpdatedReceiverTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @InjectMocks private CaseUpdatedReceiver underTest;

  @Test
  public void testReceiveCloseDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setSurvey("CENSUS");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CLOSE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtCloseActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtCloseActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtCloseActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getSurveyName()).isEqualTo("CENSUS");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CLOSE);
  }

  @Test
  public void testReceiveCreateDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("HH");
    collectionCase.setUndeliveredAsAddressed(Boolean.FALSE);
    Address address = new Address();
    address.setAddressLevel("U");
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
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actualAi.getSecureEstablishment()).isNull();
  }

  @Test
  public void testReceiveCreateDecisionCE() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setCaseRef("testRef");
    collectionCase.setCaseType("CE");
    collectionCase.setUndeliveredAsAddressed(Boolean.FALSE);

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
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("E");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actualAi.getSecureEstablishment()).isTrue();
  }

  @Test
  public void testReceiveUpdateDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setCaseRef("testRef");
    collectionCase.setUndeliveredAsAddressed(Boolean.FALSE);
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
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
  }

  @Test
  public void testReceiveUpdateDecisionCE() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setCaseRef("testRef");
    collectionCase.setUndeliveredAsAddressed(Boolean.FALSE);
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
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("E");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.UPDATE);
    assertThat(actualAi.getSecureEstablishment()).isFalse();
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
