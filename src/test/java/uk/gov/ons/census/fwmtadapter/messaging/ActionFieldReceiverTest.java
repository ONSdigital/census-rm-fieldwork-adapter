package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseMetadata;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtActionInstruction;

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
    fieldworkFollowup.setBlankQreReturned(false);
    fieldworkFollowup.setAddressType("HH");
    fieldworkFollowup.setCeActualResponses(0);
    fieldworkFollowup.setCeExpectedCapacity(null);
    fieldworkFollowup.setHandDelivery(true);

    // When
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<FwmtActionInstruction> argCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("TEST EXCHANGE"), eq(""), argCaptor.capture());

    FwmtActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "townName",
            "postcode",
            "organisationName",
            "oa",
            "uprn",
            "estabUprn",
            "estabType");
    assertThat(actionInstruction.getLatitude())
        .isEqualTo(Double.parseDouble(fieldworkFollowup.getLatitude()));
    assertThat(actionInstruction.getLongitude())
        .isEqualTo(Double.parseDouble(fieldworkFollowup.getLongitude()));
    assertThat(actionInstruction.getAddressLine1()).isEqualTo(fieldworkFollowup.getAddressLine1());
    assertThat(actionInstruction.getAddressLine2()).isEqualTo(fieldworkFollowup.getAddressLine2());
    assertThat(actionInstruction.getAddressLine3()).isEqualTo(fieldworkFollowup.getAddressLine3());

    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "caseRef",
            "surveyName",
            "addressType",
            "addressLevel",
            "fieldOfficerId");

    assertThat(actionInstruction.isCe1Complete()).isFalse();
    assertThat(actionInstruction.getCeExpectedCapacity()).isNull();
    assertThat(actionInstruction.getCeActualResponses()).isEqualTo(0);
    assertThat(actionInstruction.isHandDeliver()).isTrue();
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
    ArgumentCaptor<FwmtActionInstruction> actionInstructionArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("TEST EXCHANGE"), eq(""), actionInstructionArgumentCaptor.capture());
    FwmtActionInstruction actionRequest = actionInstructionArgumentCaptor.getValue();

    assertThat(actionRequest.isCe1Complete()).isFalse();
    assertThat(actionRequest.getCeExpectedCapacity()).isEqualTo(5);
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
    ArgumentCaptor<FwmtActionInstruction> actionInstructionArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("TEST EXCHANGE"), eq(""), actionInstructionArgumentCaptor.capture());
    FwmtActionInstruction actionRequest = actionInstructionArgumentCaptor.getValue();

    assertThat(actionRequest.isCe1Complete()).isTrue();
    assertThat(actionRequest.getCeExpectedCapacity()).isEqualTo(5);
    assertThat(actionRequest.getCeActualResponses()).isEqualTo(5);
  }

  @Test
  public void testSpgSecureEstabTrue() {
    ActionFieldReceiver underTest = new ActionFieldReceiver(rabbitTemplate, "TEST EXCHANGE");
    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setAddressType("SPG");
    fieldworkFollowup.setAddressLevel("U");

    CaseMetadata caseMetadata = new CaseMetadata();
    caseMetadata.setSecureEstablishment(true);
    fieldworkFollowup.setMetadata(caseMetadata);

    // When
    underTest.receiveMessage(fieldworkFollowup);

    // Then
    ArgumentCaptor<FwmtActionInstruction> actionInstructionArgumentCaptor =
        ArgumentCaptor.forClass(FwmtActionInstruction.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("TEST EXCHANGE"), eq(""), actionInstructionArgumentCaptor.capture());
    FwmtActionInstruction actionRequest = actionInstructionArgumentCaptor.getValue();

    assertThat(actionRequest.isSecureEstablishment()).isTrue();
  }
}
