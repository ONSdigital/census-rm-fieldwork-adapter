package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.FwmtCreateActionInstruction;

@MessageEndpoint
public class ActionFieldReceiver {
  private final String outboundExchange;
  private final RabbitTemplate rabbitTemplate;

  public ActionFieldReceiver(
      @Qualifier("specialMagicalRabbitTemplate") RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFieldInputChannel")
  public void receiveMessage(FieldworkFollowup followup) {
    FwmtCreateActionInstruction actionInstruction = new FwmtCreateActionInstruction();
    actionInstruction.setActionInstruction(ActionInstructionType.CREATE);
    actionInstruction.setAddressLevel(followup.getAddressLevel());
    actionInstruction.setAddressLine1(followup.getAddressLine1());
    actionInstruction.setAddressLine2(followup.getAddressLine2());
    actionInstruction.setAddressLine3(followup.getAddressLine3());
    actionInstruction.setAddressType(followup.getAddressType());
    actionInstruction.setCaseId(followup.getCaseId());
    actionInstruction.setCaseRef(followup.getCaseRef());
    actionInstruction.setCe1Complete(getCEComplete(followup));
    actionInstruction.setCeActualResponses(followup.getCeActualResponses());
    actionInstruction.setCeExpectedCapacity(followup.getCeExpectedCapacity());
    actionInstruction.setEstabType(followup.getEstabType());
    actionInstruction.setFieldCoordinatorId(followup.getFieldCoordinatorId());
    actionInstruction.setFieldOfficerId(followup.getFieldOfficerId());
    actionInstruction.setHandDeliver(false); // TODO: This needs to be based on treatment code
    if (followup.getLatitude() != null && !followup.getLatitude().isBlank()) {
      actionInstruction.setLatitude(Double.parseDouble(followup.getLatitude()));
    }
    if (followup.getLongitude() != null && !followup.getLongitude().isBlank()) {
      actionInstruction.setLongitude(Double.parseDouble(followup.getLongitude()));
    }
    actionInstruction.setOa(followup.getOa());
    actionInstruction.setOrganisationName(followup.getOrganisationName());
    actionInstruction.setPostcode(followup.getPostcode());
    actionInstruction.setSurveyName(followup.getSurveyName());
    actionInstruction.setTownName(followup.getTownName());
    actionInstruction.setUprn(followup.getUprn());

    // TODO: Why are we not sending these anymore?
    //    actionRequest.setUndeliveredAsAddress(followup.getUndeliveredAsAddress());
    //    actionRequest.setBlankQreReturned(followup.getBlankQreReturned());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private Boolean getCEComplete(FieldworkFollowup followup) {
    if (followup.getAddressType().equals("CE") && followup.getAddressLevel().equals("E")) {
      if (followup.getReceipted() != null) {
        return followup.getReceipted();
      }
    }

    return false;
  }

  private int getIntegerValueOrZero(Integer value) {
    if (value == null) {
      return 0;
    }

    return value;
  }
}
