package uk.gov.ons.census.fwmtadapter.util;

import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.ReceiptDTO;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

public class ReceiptHelper {

  public static ResponseManagementEvent setUpResponseManagementReceiptEvent(ReceiptDTO receiptDTO) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Payload payload = new Payload();
    payload.setReceipt(receiptDTO);
    responseManagementEvent.setPayload(payload);

    return responseManagementEvent;
  }
}
