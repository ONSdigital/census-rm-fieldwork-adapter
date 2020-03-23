package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

@MessageEndpoint
public class CaseUpdatedReceiver {

  private final RabbitTemplate rabbitTemplate;
  private final String outboundExchange;

  public CaseUpdatedReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
  }

  @Transactional
  @ServiceActivator(inputChannel = "caseUpdatedInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    if (canIgnoreEvent(event)) return;

    ActionInstructionType fieldDecision = event.getPayload().getMetadata().getFieldDecision();

    // TODO switch?
    if (fieldDecision == ActionInstructionType.CLOSE) {
      handleCloseDecision(event);
      return;
    }

    if (fieldDecision == ActionInstructionType.CREATE
        || fieldDecision == ActionInstructionType.UPDATE) {
      buildAndSendActionInstruction(event, fieldDecision);
      return;
    }

    throw new RuntimeException(
        String.format(
            "Unsupported field decision: %s", event.getPayload().getMetadata().getFieldDecision()));
  }

  private void buildAndSendActionInstruction(
      ResponseManagementEvent event, ActionInstructionType actionInstructionType) {
    CollectionCase caze = event.getPayload().getCollectionCase();

    FwmtActionInstruction actionInstruction = new FwmtActionInstruction();
    actionInstruction.setActionInstruction(actionInstructionType);
    actionInstruction.setAddressLevel(caze.getAddress().getAddressLevel());
    actionInstruction.setAddressLine1(caze.getAddress().getAddressLine1());
    actionInstruction.setAddressLine2(caze.getAddress().getAddressLine2());
    actionInstruction.setAddressLine3(caze.getAddress().getAddressLine3());
    actionInstruction.setAddressType(caze.getAddress().getAddressType());
    actionInstruction.setCaseId(caze.getId());
    actionInstruction.setCaseRef(caze.getCaseRef());
    actionInstruction.setCe1Complete(getCEComplete(caze));
    actionInstruction.setCeActualResponses(caze.getCeActualResponses());
    actionInstruction.setCeExpectedCapacity(caze.getCeExpectedCapacity());
    actionInstruction.setEstabType(caze.getAddress().getEstabType());
    actionInstruction.setFieldCoordinatorId(caze.getFieldCoordinatorId());
    actionInstruction.setFieldOfficerId(caze.getFieldOfficerId());
    actionInstruction.setHandDeliver(caze.isHandDelivery());
    if (caze.getAddress().getLatitude() != null && !caze.getAddress().getLatitude().isBlank()) {
      actionInstruction.setLatitude(Double.parseDouble(caze.getAddress().getLatitude()));
    }
    if (caze.getAddress().getLongitude() != null && !caze.getAddress().getLongitude().isBlank()) {
      actionInstruction.setLongitude(Double.parseDouble(caze.getAddress().getLongitude()));
    }
    actionInstruction.setOa(caze.getOa());
    actionInstruction.setOrganisationName(caze.getAddress().getOrganisationName());
    actionInstruction.setPostcode(caze.getAddress().getPostcode());
    actionInstruction.setSurveyName(caze.getSurvey());
    actionInstruction.setTownName(caze.getAddress().getTownName());
    actionInstruction.setUprn(caze.getAddress().getUprn());
    actionInstruction.setUndeliveredAsAddress(caze.getUndeliveredAsAddressed());

    if (caze.getMetadata() != null) {
      actionInstruction.setSecureEstablishment(caze.getMetadata().getSecureEstablishment());
    }

    actionInstruction.setBlankFormReturned(event.getPayload().getMetadata().getBlankQuestionnaireReceived());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private void handleCloseDecision(ResponseManagementEvent event) {
    FwmtCloseActionInstruction actionInstruction = new FwmtCloseActionInstruction();
    actionInstruction.setActionInstruction(ActionInstructionType.CLOSE);
    actionInstruction.setAddressLevel(
        event.getPayload().getCollectionCase().getAddress().getAddressLevel());
    actionInstruction.setAddressType(
        event.getPayload().getCollectionCase().getAddress().getAddressType());
    actionInstruction.setCaseId(event.getPayload().getCollectionCase().getId());
    actionInstruction.setSurveyName(event.getPayload().getCollectionCase().getSurvey());

    // These have been added in because we can't guarantee the order that we would publish separate
    // UPDATE and CLOSE messages, which would be published simultaneously
    actionInstruction.setCeActualResponses(
        event.getPayload().getCollectionCase().getCeActualResponses());
    actionInstruction.setCeExpectedCapacity(
        event.getPayload().getCollectionCase().getCeExpectedCapacity());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private boolean canIgnoreEvent(ResponseManagementEvent event) {
    return event.getPayload().getMetadata() == null
        || event.getPayload().getMetadata().getFieldDecision() == null;
  }

  private Boolean getCEComplete(CollectionCase caze) {
    if (caze.getAddress().getAddressType().equals("CE")
        && caze.getAddress().getAddressLevel().equals("E")
        && caze.getReceiptReceived() != null) {
      return caze.getReceiptReceived();
    }

    return false;
  }
}
