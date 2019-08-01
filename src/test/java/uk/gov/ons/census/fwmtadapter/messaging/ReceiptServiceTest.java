package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.ons.census.fwmtadapter.services.ReceiptService.RECEIPTED;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.Receipt;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.services.CaseService;
import uk.gov.ons.census.fwmtadapter.services.ReceiptService;

public class ReceiptServiceTest {

  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_QID = "test_qid";

  @Test
  public void testReceiptWithCaseId() {
    // Given
    Receipt receipt = new Receipt();
    receipt.setCaseId(TEST_CASE_ID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    ReceiptService receiptService = new ReceiptService(rabbitTemplate, "exchange_name", null);

    // When
    receiptService.processReceipt(receipt);

    // then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange_name"), eq(""), argCaptor.capture());
    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getReason()).isEqualTo(RECEIPTED);
  }

  @Test
  public void testReceiptWithQidAndNoCaseId() {
    // Given
    Receipt receipt = new Receipt();
    receipt.setQuestionnaire_Id(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseService caseService = mock(CaseService.class);
    when(caseService.getCaseIdFromQid(TEST_QID)).thenReturn(TEST_CASE_ID);

    ReceiptService receiptService =
        new ReceiptService(rabbitTemplate, "exchange_name", caseService);

    receiptService.processReceipt(receipt);

    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange_name"), eq(""), argCaptor.capture());
    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getReason()).isEqualTo(RECEIPTED);
  }

  @Test(expected = RuntimeException.class)
  public void testReceiptWithJustQidButNotFoundThroughCaseService() {
    Receipt receipt = new Receipt();
    receipt.setQuestionnaire_Id(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseService caseService = mock(CaseService.class);
    when(caseService.getCaseIdFromQid(TEST_QID)).thenThrow(new RuntimeException());

    ReceiptService receiptService =
        new ReceiptService(rabbitTemplate, "exchange_name", caseService);

    receiptService.processReceipt(receipt);
  }
}
