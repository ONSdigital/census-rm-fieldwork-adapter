package uk.gov.ons.census.fwmtadapter.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

@MessageEndpoint
public class CaseUpdatedReceiver {

  private static final Logger log = LoggerFactory.getLogger(CaseUpdatedReceiver.class);

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

    if (event.getPayload().getMetadata().getFieldDecision() == ActionInstructionType.CLOSE) {
      handleCloseDecision(event);
    }
    return;
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
}
