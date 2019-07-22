package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class ActionFieldReceiverTest {

  @Test
  public void testReceiveMessage() {
    // Given
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    // When
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");
    EasyRandom easyRandom = new EasyRandom();
    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setCeExpectedCapacity("999");
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(fieldworkFollowup.getAddressLine1())
        .isEqualTo(actionInstruction.getActionRequest().getAddress().getLine1());
    assertThat(fieldworkFollowup.getPostcode())
        .isEqualTo(actionInstruction.getActionRequest().getAddress().getPostcode());
    assertThat(new BigDecimal("-179.99999"))
        .isEqualTo(actionInstruction.getActionRequest().getAddress().getLatitude());
    assertThat(new BigDecimal("179.99999"))
        .isEqualTo(actionInstruction.getActionRequest().getAddress().getLongitude());
    assertThat(999).isEqualTo(actionInstruction.getActionRequest().getCeExpectedResponses());
  }
}
