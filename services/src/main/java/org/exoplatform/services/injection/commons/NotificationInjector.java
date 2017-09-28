package org.exoplatform.services.injection.commons;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.scheduler.JobSchedulerService;

/**
 * Created by ngammoudi on 9/19/17.
 */
@Path("/queuetest")
public class NotificationInjector implements ResourceContainer {
    private static final Log LOG = ExoLogger.getExoLogger(NotificationInjector.class);
    private JobSchedulerService jobSchedulerService;
    private QueueMessage queueMessage;
    private int batchSize = 0;

    public NotificationInjector(JobSchedulerService jobSchedulerService, QueueMessage queueMessage) {
        this.jobSchedulerService = jobSchedulerService;
        this.queueMessage = queueMessage;
        batchSize = Integer.parseInt(System.getProperty("conf.notification.service.QueueMessage.numberOfMailPerBatch").trim());
    }

    @GET
    public void get() {
        jobSchedulerService.suspend();
        try {
            List<MessageInfo> messageInfos = new ArrayList<>();
            for (int i = 0; i < (50 * batchSize); i++) {
                MessageInfo messageInfo = new MessageInfo();
                messageInfo.subject("Subject " + i + " To send").body("Content " + i + "To Send").pluginId("pluginId").from("test" + i + "@localhost.com").to("test2@localhost.com").end();
                messageInfo.setId(NotificationInfo.PREFIX_ID + IdGenerator.generate());
                messageInfos.add(messageInfo);
            }

            long startTime = System.currentTimeMillis();
            for (MessageInfo messageInfo : messageInfos) {
                queueMessage.put(messageInfo);
            }
            long endTime = System.currentTimeMillis();

            LOG.info("************* PUT Time = " + (endTime - startTime));
        } finally {
            jobSchedulerService.resume();
        }
    }

    @GET
    @Path("send")
    public void send() {
        jobSchedulerService.suspend();
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 50; i++) {
                queueMessage.send();
            }
            long endTime = System.currentTimeMillis();
            LOG.info("************* SEND Time = " + (endTime - startTime));
        } finally {
            jobSchedulerService.resume();
        }
    }

}