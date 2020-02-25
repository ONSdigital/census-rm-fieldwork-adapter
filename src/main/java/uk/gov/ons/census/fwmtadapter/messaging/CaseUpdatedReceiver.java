package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCreateActionInstruction;

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

    if (fieldDecision == ActionInstructionType.CLOSE) {
      handleCloseDecision(event);
      return;
    } else if (fieldDecision == ActionInstructionType.CREATE) {
      handleCreateDecision(event);
      return;
    }
    throw new RuntimeException(
        String.format(
            "Unsupported field decision: %s", event.getPayload().getMetadata().getFieldDecision()));
  }

  private void handleCreateDecision(ResponseManagementEvent event) {
    CollectionCase caze = event.getPayload().getCollectionCase();

    FwmtCreateActionInstruction actionInstruction = new FwmtCreateActionInstruction();
    actionInstruction.setActionInstruction(ActionInstructionType.CREATE);
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
    actionInstruction.setCaseRef(event.getPayload().getCollectionCase().getCaseRef());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private boolean canIgnoreEvent(ResponseManagementEvent event) {
    return event.getPayload().getMetadata() == null
        || event.getPayload().getMetadata().getFieldDecision() == null;
  }

  private Boolean getCEComplete(CollectionCase caze) {
    if (caze.getAddress().getAddressType().equals("CE")
        && caze.getAddress().getAddressLevel().equals("E")) {
      if (caze.getReceiptReceived() != null) {
        return caze.getReceiptReceived();
      }
    }

    return false;
  }
}
