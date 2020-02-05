package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

@MessageEndpoint
public class RefusalReceiver {
  private final String outboundExchange;
  private final CaseClient caseClient;
  private final RabbitTemplate rabbitTemplate;

  private static final String ESTAB_ADDRESS_LEVEL = "E";
  private static final String FIELD_CHANNEL = "FIELD";

  public RefusalReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseClient caseClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseClient = caseClient;
  }

  @Transactional
  @ServiceActivator(inputChannel = "refusalInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    if (event.getEvent().getType() != EventType.REFUSAL_RECEIVED) {
      throw new RuntimeException(
          String.format("Event Type '%s' is invalid!", event.getEvent().getType()));
    }

    // Do not send refusal back to Field if from Field
    if (FIELD_CHANNEL.equalsIgnoreCase(event.getEvent().getChannel())) {
      return;
    }

    String caseId = event.getPayload().getRefusal().getCollectionCase().getId();

    CaseContainerDto caseContainer = caseClient.getCaseFromCaseId(caseId);

    // Ignore refusal if estab level case - we shouldn't get these from any channel except Field
    if (caseContainer.getAddressLevel().equals(ESTAB_ADDRESS_LEVEL)) {
      return;
    }

    FwmtCloseActionInstruction actionInstruction = new FwmtCloseActionInstruction();
    actionInstruction.setActionInstruction(ActionInstructionType.CLOSE);
    actionInstruction.setAddressLevel(caseContainer.getAddressLevel());
    actionInstruction.setAddressType(caseContainer.getAddressType());
    actionInstruction.setCaseId(caseContainer.getCaseId());
    actionInstruction.setCaseRef(caseContainer.getCaseRef());

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}
