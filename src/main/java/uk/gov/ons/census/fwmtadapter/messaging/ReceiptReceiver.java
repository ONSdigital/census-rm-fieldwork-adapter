package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.Receipt;
import uk.gov.ons.census.fwmtadapter.services.ReceiptService;

@MessageEndpoint
public class ReceiptReceiver {
  private final ReceiptService receiptService;

  public ReceiptReceiver(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "receiptedChannel")
  public void receiveMessage(Receipt receipt) {
    receiptService.processReceipt(receipt);
  }
}
