package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

public class InvalidAddressReceiverTest {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testInvalidAddressFromNonFieldChannel() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.ADDRESS_NOT_VALID);
    event.getEvent().setChannel("CC");
    event.getPayload().getInvalidAddress().getCollectionCase().setId(TEST_CASE_ID);

    // When
    CaseClient caseClient = mock(CaseClient.class);
    CaseContainerDto caseContainerDto = new CaseContainerDto();
    caseContainerDto.setCaseId(TEST_CASE_ID);
    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
    when(caseClient.getCaseFromCaseId(TEST_CASE_ID)).thenReturn(caseContainerDto);

    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", caseClient);

    underTest.receiveMessage(event);

    // Then
    ArgumentCaptor<FwmtCloseActionInstruction> argCaptor =
        ArgumentCaptor.forClass(FwmtCloseActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    FwmtCloseActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(TEST_CASE_ID).isEqualTo(actionInstruction.getCaseId());
    assertThat(actionInstruction.getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }

  @Test
  public void testRefusalMessageFromFieldChannel() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.ADDRESS_NOT_VALID);
    event.getEvent().setChannel("FIELD");
    underTest.receiveMessage(event);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test(expected = RuntimeException.class)
  public void shouldThrowRuntimeExceptionWhenInvalidEventTypeExpected() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.CASE_CREATED);
    String expectedErrorMessage =
        String.format("Event Type '%s' is invalid on this topic", EventType.CASE_CREATED);

    try {
      // When
      underTest.receiveMessage(event);
    } catch (RuntimeException re) {
      // Then
      assertThat(re.getMessage()).isEqualTo(expectedErrorMessage);
      throw re;
    }
  }

  @Test
  public void testAddressModifiedIsIgnored() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.ADDRESS_MODIFIED);
    event.getEvent().setChannel("CC");
    underTest.receiveMessage(event);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test
  public void testAddressTypeChangedIsIgnored() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.ADDRESS_TYPE_CHANGED);
    event.getEvent().setChannel("CC");
    underTest.receiveMessage(event);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test
  public void testNewAddressIgnored() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    InvalidAddressReceiver underTest =
        new InvalidAddressReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.NEW_ADDRESS_REPORTED);
    event.getEvent().setChannel("CC");
    underTest.receiveMessage(event);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }
}
