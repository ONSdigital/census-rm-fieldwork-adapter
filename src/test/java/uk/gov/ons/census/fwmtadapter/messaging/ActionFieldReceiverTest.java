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
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionRequest;

public class ActionFieldReceiverTest {
  private static final EasyRandom easyRandom = new EasyRandom();
  private RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

  @Test
  public void testReceiveMessage() {
    // Given
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");

    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setSurveyName("CENSUS");
    fieldworkFollowup.setUndeliveredAsAddress(false);
    fieldworkFollowup.setBlankQreReturned(false);
    fieldworkFollowup.setAddressType("HH");
    fieldworkFollowup.setCeActualResponses(null);
    fieldworkFollowup.setCeExpectedCapacity(null);

    // When
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionRequest().getAddress())
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "townName",
            "postcode",
            "organisationName",
            "oa",
            "arid",
            "uprn",
            "estabType");
    assertThat(actionInstruction.getActionRequest().getAddress().getLatitude())
        .isEqualTo(new BigDecimal(fieldworkFollowup.getLatitude()));
    assertThat(actionInstruction.getActionRequest().getAddress().getLongitude())
        .isEqualTo(new BigDecimal(fieldworkFollowup.getLongitude()));
    assertThat(actionInstruction.getActionRequest().getAddress().getLine1())
        .isEqualTo(fieldworkFollowup.getAddressLine1());
    assertThat(actionInstruction.getActionRequest().getAddress().getLine2())
        .isEqualTo(fieldworkFollowup.getAddressLine2());
    assertThat(actionInstruction.getActionRequest().getAddress().getLine3())
        .isEqualTo(fieldworkFollowup.getAddressLine3());

    assertThat(actionInstruction.getActionRequest())
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "actionPlan",
            "actionType",
            "caseId",
            "caseRef",
            "surveyName",
            "addressType",
            "addressLevel",
            "fieldOfficerId",
            "undeliveredAsAddress",
            "blankQreReturned");
    assertThat(actionInstruction.getActionRequest().getTreatmentId())
        .isEqualTo(fieldworkFollowup.getTreatmentCode());

    assertThat(actionInstruction.getActionRequest().getCeCE1Complete()).isFalse();
    assertThat(actionInstruction.getActionRequest().getCeExpectedResponses()).isEqualTo(0);
    assertThat(actionInstruction.getActionRequest().getCeActualResponses()).isEqualTo(0);
  }

  @Test
  public void testCommunityEstabCE1CompleteFalse() {
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");

    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setAddressType("CE");
    fieldworkFollowup.setAddressLevel("E");
    fieldworkFollowup.setReceipted(false);
    fieldworkFollowup.setCeExpectedCapacity(5);
    fieldworkFollowup.setCeActualResponses(0);

    // When
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<ActionInstruction> actionInstructionArgumentCaptor =
        ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("TEST EXCHANGE"), eq(""), actionInstructionArgumentCaptor.capture());
    ActionRequest actionRequest = actionInstructionArgumentCaptor.getValue().getActionRequest();

    assertThat(actionRequest.getCeCE1Complete()).isFalse();
    assertThat(actionRequest.getCeExpectedResponses()).isEqualTo(5);
    assertThat(actionRequest.getCeActualResponses()).isEqualTo(0);
  }

  @Test
  public void testCommunityEstabCE1CompleteTrue() {
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");
    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setAddressType("CE");
    fieldworkFollowup.setAddressLevel("E");
    fieldworkFollowup.setReceipted(true);
    fieldworkFollowup.setCeExpectedCapacity(5);
    fieldworkFollowup.setCeActualResponses(5);

    // When
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<ActionInstruction> actionInstructionArgumentCaptor =
        ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("TEST EXCHANGE"), eq(""), actionInstructionArgumentCaptor.capture());
    ActionRequest actionRequest = actionInstructionArgumentCaptor.getValue().getActionRequest();

    assertThat(actionRequest.getCeCE1Complete()).isTrue();
    assertThat(actionRequest.getCeExpectedResponses()).isEqualTo(5);
    assertThat(actionRequest.getCeActualResponses()).isEqualTo(5);
  }
}
