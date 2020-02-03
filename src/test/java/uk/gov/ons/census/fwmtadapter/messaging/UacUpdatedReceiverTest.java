package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.Event;
import uk.gov.ons.census.fwmtadapter.model.dto.FwmtCloseActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.Uac;

@RunWith(MockitoJUnitRunner.class)
public class UacUpdatedReceiverTest {
  @Mock private RabbitTemplate rabbitTemplate;

  @Mock private CaseClient caseClient;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @InjectMocks private UacUpdatedReceiver underTest;

  @Test
  public void testReceiveMessage() {
    // Given
    Uac uac = new Uac();
    uac.setActive(false);
    uac.setCaseId("test case ID");
    uac.setQuestionnaireId("0120000000000100");
    Payload payload = new Payload();
    payload.setUac(uac);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    CaseContainerDto caseContainerDto = new CaseContainerDto();
    caseContainerDto.setAddressType("test address type");
    caseContainerDto.setCaseId("test case ID");
    when(caseClient.getCaseFromCaseId(any())).thenReturn(caseContainerDto);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(caseClient).getCaseFromCaseId(eq("test case ID"));

    ArgumentCaptor<FwmtCloseActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtCloseActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtCloseActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo("test case ID");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
  }

  @Test
  public void testReceiveMessageActiveUac() {
    // Given
    Uac uac = new Uac();
    uac.setActive(true);
    Payload payload = new Payload();
    payload.setUac(uac);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(caseClient, never()).getCaseFromCaseId(any());
    verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
  }

  @Test
  public void testReceiveMessageUnlinkedUacQid() {
    // Given
    Uac uac = new Uac();
    uac.setActive(false);
    uac.setCaseId(null);
    uac.setQuestionnaireId("test questionnaire id");
    Payload payload = new Payload();
    payload.setUac(uac);
    Event event = new Event();
    event.setTransactionId("test transaction id");
    event.setChannel("test channel");
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    responseManagementEvent.setEvent(event);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(caseClient, never()).getCaseFromCaseId(any());
    verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
  }

  @Test
  public void testReceivedMessageContinuationQuestionnaireQid() {
    // Given
    Uac uac = new Uac();
    uac.setActive(false);
    uac.setCaseId(null);
    String englandHouseholdContinuationQid = "1120000000000100";
    uac.setQuestionnaireId(englandHouseholdContinuationQid);

    Payload payload = new Payload();
    payload.setUac(uac);
    Event event = new Event();
    event.setTransactionId("test transaction id");
    event.setChannel("test channel");
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    responseManagementEvent.setEvent(event);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(caseClient, never()).getCaseFromCaseId(any());
    verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
  }
}
