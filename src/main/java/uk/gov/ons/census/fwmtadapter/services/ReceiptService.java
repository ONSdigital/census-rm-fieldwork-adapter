package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@Service
public class ReceiptService {

  public static final String RECEIPTED = "RECEIPTED";
  private final RabbitTemplate rabbitTemplate;
  private final String outboundExchange;
  private final CaseClient caseClient;

  public ReceiptService(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseClient caseClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseClient = caseClient;
  }

  public void processReceipt(ResponseManagementEvent receiptEvent) {
    ActionCancel actionCancel = new ActionCancel();

    CaseContainerDto caseContainerDto =
        caseClient.getCaseFromQid(receiptEvent.getPayload().getReceipt().getQuestionnaireId());

    actionCancel.setCaseId(caseContainerDto.getCaseId());
    actionCancel.setAddressType(caseContainerDto.getAddressType());
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}
