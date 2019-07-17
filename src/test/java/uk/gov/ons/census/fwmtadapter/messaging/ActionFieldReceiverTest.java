package uk.gov.ons.census.fwmtadapter.messaging;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class ActionFieldReceiverTest {

  @Test
  public void testReceiveMessage() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");
    ActionInstruction actionInstruction = new ActionInstruction();
    underTest.receiveMessage(actionInstruction);

    // Then
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), eq(actionInstruction));
  }
}
