package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@Service
public class ReceiptService {

  public static final String RECEIPTED = "RECEIPTED";
  private final RabbitTemplate rabbitTemplate;
  private final String outboundExchange;
  private final CaseService caseService;

  public ReceiptService(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseService caseService) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseService = caseService;
  }

  public void processReceipt(ResponseManagementEvent receiptEvent) {
    System.out.println("Processing Receipt");

    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(getCaseId(receiptEvent));
    actionCancel.setReason(RECEIPTED);
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private String getCaseId(ResponseManagementEvent receiptEvent) {
    String caseId = receiptEvent.getPayload().getReceipt().getCaseId();

    // Before being sent to us from pubsub, if the caseId is null, it gets set to 0
    if (caseId == null || caseId.isBlank() || caseId.equals("0")) {
      caseId =
          caseService.getCaseIdFromQid(receiptEvent.getPayload().getReceipt().getQuestionnaireId());
    }

    return caseId;
  }
}
