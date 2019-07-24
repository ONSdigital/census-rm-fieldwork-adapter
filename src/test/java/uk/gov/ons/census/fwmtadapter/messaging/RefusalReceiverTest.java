package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class RefusalReceiverTest {

  @Test
  public void testReceiveMessage() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    RefusalReceiver underTest = new RefusalReceiver(rabbitTemplate, "TEST EXCHANGE");
    EasyRandom easyRandom = new EasyRandom();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.REFUSAL_RECEIVED);
    underTest.receiveMessage(event);

    // Then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(event.getPayload().getRefusal().getCollectionCase().getId())
        .isEqualTo(actionInstruction.getActionCancel().getCaseId());
    assertThat("REFUSED").isEqualTo(actionInstruction.getActionCancel().getReason());
  }
}
