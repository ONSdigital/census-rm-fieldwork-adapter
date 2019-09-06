package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@MessageEndpoint
public class InvalidAddressReceiver {
  private final String outboundExchange;
  private final CaseClient caseClient;
  private final RabbitTemplate rabbitTemplate;

  public InvalidAddressReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseClient caseClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseClient = caseClient;
  }

  @Transactional
  @ServiceActivator(inputChannel = "invalidAddressInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    switch (event.getEvent().getType()) {
      case ADDRESS_MODIFIED:
      case ADDRESS_TYPE_CHANGED:
      case NEW_ADDRESS_REPORTED:
        return; // We don't do anything with these
      case ADDRESS_NOT_VALID:
        break; // We DO WANT to process this one
      default:
        throw new RuntimeException(
            String.format("Event Type '%s' is invalid on this topic", event.getEvent().getType()));
    }

    // Do not send back to Field if from Field
    if ("FIELD".equalsIgnoreCase(event.getEvent().getChannel())) {
      return;
    }

    String caseId = event.getPayload().getInvalidAddress().getCollectionCase().getId();

    CaseContainerDto caseContainerDto = caseClient.getCaseFromCaseId(caseId);

    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(caseId);
    actionCancel.setAddressType(caseContainerDto.getAddressType());
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}
