package uk.gov.ons.census.fwmtadapter.messaging;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionAddress;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionRequest;

@MessageEndpoint
public class ActionFieldReceiver {
  private final String outboundExchange;
  private final RabbitTemplate rabbitTemplate;

  public ActionFieldReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFieldInputChannel")
  public void receiveMessage(FieldworkFollowup followup) {
    ActionAddress actionAddress = new ActionAddress();
    actionAddress.setLine1(followup.getAddressLine1());
    actionAddress.setLine2(followup.getAddressLine2());
    actionAddress.setLine3(followup.getAddressLine3());
    actionAddress.setTownName(followup.getTownName());
    actionAddress.setPostcode(followup.getPostcode());
    actionAddress.setEstabType(followup.getEstabType());
    actionAddress.setOrganisationName(followup.getOrganisationName());
    actionAddress.setArid(followup.getArid());
    actionAddress.setUprn(followup.getUprn());
    actionAddress.setOa(followup.getOa());
    if (followup.getLatitude() != null && !followup.getLatitude().isBlank()) {
      actionAddress.setLatitude(new BigDecimal(followup.getLatitude()));
    }
    if (followup.getLongitude() != null && !followup.getLongitude().isBlank()) {
      actionAddress.setLongitude(new BigDecimal(followup.getLongitude()));
    }

    ActionRequest actionRequest = new ActionRequest();
    actionRequest.setActionId(UUID.randomUUID().toString());
    actionRequest.setResponseRequired(false);
    actionRequest.setActionPlan(followup.getActionPlan());
    actionRequest.setActionType(followup.getActionType());
    actionRequest.setAddress(actionAddress);
    actionRequest.setCaseId(followup.getCaseId());
    actionRequest.setCaseRef(followup.getCaseRef());
    actionRequest.setAddressType(followup.getAddressType());
    actionRequest.setAddressLevel(followup.getAddressLevel());
    actionRequest.setTreatmentId(followup.getTreatmentCode());
    actionRequest.setFieldOfficerId(followup.getFieldOfficerId());
    actionRequest.setCoordinatorId(followup.getFieldCoordinatorId());

    actionRequest.setCeExpectedResponses(getIntegerValueOrZero(followup.getCeExpectedCapacity()));
    actionRequest.setCeActualResponses(getIntegerValueOrZero(followup.getCeActualResponses()));
    actionRequest.setCeCE1Complete(getCEComplete(followup));

    actionRequest.setUndeliveredAsAddress(followup.getUndeliveredAsAddress());
    actionRequest.setBlankQreReturned(followup.getBlankQreReturned());
    actionRequest.setSurveyName(followup.getSurveyName());

    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionRequest(actionRequest);

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
