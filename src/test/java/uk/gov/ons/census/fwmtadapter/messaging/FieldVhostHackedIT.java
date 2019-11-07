package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.Event;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.util.FieldRabbitQueueHelper;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class FieldVhostHackedIT {

  @Autowired private FieldRabbitQueueHelper fieldRabbitQueueHelper;
  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue("RM.Field");
  }

  @Test
  public void testReceiveMessage() throws InterruptedException {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Event event = new Event();

    String testID =
        "A test Id that shows passing from a field_vhost receiver to / vhost exchange/queue";

    event.setTransactionId(testID);
    responseManagementEvent.setEvent(event);

    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen("RM.Field");

    fieldRabbitQueueHelper.sendMessage("FIELD.queue", responseManagementEvent);

    String msgJson = rabbitQueueHelper.getMessage(outboundQueue);

    assertThat(msgJson).contains(testID);
  }
}
