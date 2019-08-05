package uk.gov.ons.census.fwmtadapter.messaging;

import static org.mockito.Mockito.*;
import static uk.gov.ons.census.fwmtadapter.util.ReceiptHelper.setUpResponseManagementReceiptEvent;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import uk.gov.ons.census.fwmtadapter.model.dto.ReceiptDTO;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.services.ReceiptService;

public class ReceiptReceiverTest {

  @Test
  public void testReceipting() {
    ReceiptService receiptProcessor = mock(ReceiptService.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("channel", "any receipt channel");
    headers.put("source", "any receipt source");

    ReceiptReceiver receiptReceiver = new ReceiptReceiver(receiptProcessor);
    ReceiptDTO receiptDTO = new ReceiptDTO();
    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);
    receiptReceiver.receiveMessage(responseManagementEvent);

    verify(receiptProcessor, times(1)).processReceipt(responseManagementEvent);
  }
}
