package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.FwmtCloseActionInstruction;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

@MessageEndpoint
public class InvalidAddressReceiver {
  private final String outboundExchange;
  private final CaseClient caseClient;
  private final RabbitTemplate rabbitTemplate;

  public InvalidAddressReceiver(
      @Qualifier("specialMagicalRabbitTemplate") RabbitTemplate rabbitTemplate,
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

    CaseContainerDto caseContainer = caseClient.getCaseFromCaseId(caseId);

    FwmtCloseActionInstruction actionInstruction = new FwmtCloseActionInstruction();
    actionInstruction.setActionInstruction(ActionInstructionType.CLOSE);
    actionInstruction.setAddressLevel(caseContainer.getAddressLevel());
    actionInstruction.setAddressType(caseContainer.getAddressType());
    actionInstruction.setCaseId(caseContainer.getCaseId());
    actionInstruction.setCaseRef(caseContainer.getCaseRef());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}
