package io.github.eltorres.seguirviendo;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class SyncJobService extends JobService {
    private Thread running;

    @Override
    public boolean onStartJob(JobParameters params) {
        Services services = Services.get(this);
        running = new Thread(() -> {
            SyncReport report = services.sync.syncAll();
            services.settings.setLastSyncMs(report.finishedAtMs());
            boolean retry = params.getJobId() == SyncScheduler.SOON_JOB
                    && report.outcomes().containsValue(SyncReport.Outcome.NETWORK_ERROR);
            jobFinished(params, retry);
        }, "seguir-viendo-sync");
        running.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (running != null) running.interrupt();
        return params.getJobId() == SyncScheduler.SOON_JOB;
    }
}
