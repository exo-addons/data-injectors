package org.exoplatform.services.injection.scheduler;

import org.exoplatform.services.injection.DataInjector;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DataInjectorScheduler implements Job {

    private static final Log LOG= ExoLogger.getLogger(DataInjectorScheduler.class);

    private DataInjector dataInjector;

    public DataInjectorScheduler(DataInjector dataInjector) {
        this.dataInjector = dataInjector;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            this.dataInjector.inject();
        } catch (Exception e) {
            LOG.error("Data Injection Failed", e);
        }

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
