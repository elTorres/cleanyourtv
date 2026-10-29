package io.github.eltorres.seguirviendo;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

final class SyncScheduler {
    static final int PERIODIC_JOB = 1;
    static final int SOON_JOB = 2;
    static final long PERIOD_MS = 60L * 60 * 1000;

    static final class Spec {
        final int id;
        final long periodMs;
        final boolean requiresNetwork;
        final boolean persisted;

        Spec(int id, long periodMs, boolean requiresNetwork, boolean persisted) {
            this.id = id;
            this.periodMs = periodMs;
            this.requiresNetwork = requiresNetwork;
            this.persisted = persisted;
        }
    }

    private SyncScheduler() {
    }

    static Spec periodic() {
        return new Spec(PERIODIC_JOB, PERIOD_MS, true, true);
    }

    static Spec soon() {
        return new Spec(SOON_JOB, 0, true, false);
    }

    static void schedule(Context context) {
        if (jobs(context).getPendingJob(PERIODIC_JOB) == null) submit(context, periodic());
    }

    static void syncSoon(Context context) {
        submit(context, soon());
    }

    static void cancel(Context context) {
        jobs(context).cancel(PERIODIC_JOB);
        jobs(context).cancel(SOON_JOB);
    }

    private static void submit(Context context, Spec spec) {
        JobInfo.Builder job = new JobInfo.Builder(spec.id, new ComponentName(context, SyncJobService.class))
                .setPersisted(spec.persisted);
        if (spec.requiresNetwork) job.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if (spec.periodMs > 0) job.setPeriodic(spec.periodMs);
        jobs(context).schedule(job.build());
    }

    private static JobScheduler jobs(Context context) {
        return context.getSystemService(JobScheduler.class);
    }
}
