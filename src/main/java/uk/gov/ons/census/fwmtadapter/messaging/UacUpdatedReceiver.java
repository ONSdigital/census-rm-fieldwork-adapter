//package uk.gov.ons.census.fwmtadapter.messaging;
//
//import static uk.gov.ons.census.fwmtadapter.utility.QuestionnaireTypeHelper.isContinuationQuestionnaireType;
//
//import com.godaddy.logging.Logger;
//import com.godaddy.logging.LoggerFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.integration.annotation.MessageEndpoint;
//import org.springframework.integration.annotation.ServiceActivator;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//import uk.gov.ons.census.fwmtadapter.client.CaseClient;
//import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
//import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
//import uk.gov.ons.census.fwmtadapter.model.dto.Uac;
//import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
//import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;
//
//@MessageEndpoint
//public class UacUpdatedReceiver {
//
//  private static final Logger log = LoggerFactory.getLogger(UacUpdatedReceiver.class);
//
//  private final RabbitTemplate rabbitTemplate;
//  private final String outboundExchange;
//  private final CaseClient caseClient;
//
//  public UacUpdatedReceiver(
//      RabbitTemplate rabbitTemplate,
//      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
//      CaseClient caseClient) {
//    this.rabbitTemplate = rabbitTemplate;
//    this.outboundExchange = outboundExchange;
//    this.caseClient = caseClient;
//  }
//
//  @Transactional
//  @ServiceActivator(inputChannel = "uacUpdatedInputChannel")
//  public void receiveMessage(ResponseManagementEvent event) {
//    if (canIgnoreEvent(event)) return;
//
//    CaseContainerDto caseContainer =
//        caseClient.getCaseFromCaseId(event.getPayload().getUac().getCaseId());
//
//    FwmtCloseActionInstruction actionInstruction = new FwmtCloseActionInstruction();
//    actionInstruction.setActionInstruction(ActionInstructionType.CLOSE);
//    actionInstruction.setAddressLevel(caseContainer.getAddressLevel());
//    actionInstruction.setAddressType(caseContainer.getAddressType());
//    actionInstruction.setCaseId(caseContainer.getCaseId());
//    actionInstruction.setCaseRef(caseContainer.getCaseRef());
//
//    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
//  }
//
//  private boolean canIgnoreEvent(ResponseManagementEvent event) {
//    Uac uac = event.getPayload().getUac();
//    if (StringUtils.isEmpty(uac.getCaseId())) {
//      log.with("qid", uac.getQuestionnaireId())
//          .warn("We would like to cancel the associated case but it's not been linked yet");
//      return true;
//    }
//    return uac.isActive() || isContinuationQuestionnaireType(uac.getQuestionnaireId());
//  }
//}
