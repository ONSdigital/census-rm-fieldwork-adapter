package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.concurrent.BlockingQueue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.Receipt;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiptReceiverIT {
  private static final String TEST_CASE_ID = "test_case_id";

  @Value("${queueconfig.receipt-queue}")
  private String receiptQueue;

  @Value("${queueconfig.adapter-outbound-queue}")
  private String actionOutboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(receiptQueue);
    rabbitQueueHelper.purgeQueue(actionOutboundQueue);
  }

  @Test
  public void testGoodReceiptMessage() throws InterruptedException, JAXBException {
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);
    Receipt receipt = new Receipt();
    receipt.setCaseId(TEST_CASE_ID);

    rabbitQueueHelper.sendMessage(receiptQueue, receipt);

    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
  }
}
