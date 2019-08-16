package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class RefusalReceiverTest {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_ADDRESS_TYPE = "test_address_type";
  private EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testRefusalMessageFromNonFieldChannel() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.REFUSAL_RECEIVED);
    event.getEvent().setChannel("CC");
    event.getPayload().getRefusal().getCollectionCase().setId(TEST_CASE_ID);

    // When
    CaseClient caseClient = mock(CaseClient.class);
    CaseContainerDto caseContainerDto = new CaseContainerDto();
    caseContainerDto.setCaseId(TEST_CASE_ID);
    caseContainerDto.setAddressType(TEST_ADDRESS_TYPE);
    when(caseClient.getCaseFromCaseId(TEST_CASE_ID)).thenReturn(caseContainerDto);

    RefusalReceiver underTest = new RefusalReceiver(rabbitTemplate, "TEST EXCHANGE", caseClient);

    underTest.receiveMessage(event);

    // Then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(event.getPayload().getRefusal().getCollectionCase().getId())
        .isEqualTo(actionInstruction.getActionCancel().getCaseId());
    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }

  @Test
  public void testRefusalMessageFromFieldChannel() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    RefusalReceiver underTest = new RefusalReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.REFUSAL_RECEIVED);
    event.getEvent().setChannel("FIELD");
    underTest.receiveMessage(event);

    // Then
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test(expected = RuntimeException.class)
  public void shouldThrowRuntimeExceptionWhenInvalidEventTypeExpected() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    RefusalReceiver underTest = new RefusalReceiver(rabbitTemplate, "TEST EXCHANGE", null);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.CASE_CREATED);
    String expectedErrorMessage =
        String.format("Event Type '%s' is invalid!", EventType.CASE_CREATED);

    try {
      // When
      underTest.receiveMessage(event);
    } catch (RuntimeException re) {
      // Then
      assertThat(re.getMessage()).isEqualTo(expectedErrorMessage);
      throw re;
    }
  }
}
