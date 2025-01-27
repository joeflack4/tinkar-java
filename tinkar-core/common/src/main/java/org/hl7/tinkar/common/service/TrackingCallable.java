package org.hl7.tinkar.common.service;

import org.hl7.tinkar.common.util.time.DurationUtil;
import org.hl7.tinkar.common.util.time.Stopwatch;

import java.time.Duration;
import java.util.concurrent.Callable;

public abstract class TrackingCallable<V> implements Callable<V> {
    final boolean allowUserCancel;
    final boolean retainWhenComplete;
    Stopwatch stopwatch = new Stopwatch();
    TrackingListener listener;
    double workDone;
    double maxWork;
    double updateThreshold = 0.005;
    String title;
    String message;
    V value;
    boolean isCancelled = false;

    public TrackingCallable() {
        this.allowUserCancel = true;
        this.retainWhenComplete = false;
    }

    public TrackingCallable(boolean allowUserCancel, boolean retainWhenComplete) {
        this.allowUserCancel = allowUserCancel;
        this.retainWhenComplete = retainWhenComplete;
    }

    public TrackingCallable(boolean allowUserCancel) {
        this.allowUserCancel = allowUserCancel;
        this.retainWhenComplete = false;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void cancel() {
        this.isCancelled = true;
    }

    public boolean allowUserCancel() {
        return allowUserCancel;
    }

    public boolean updateIntervalElapsed() {
        return stopwatch.updateIntervalElapsed();
    }

    public boolean retainWhenComplete() {
        return retainWhenComplete;
    }

    @Override
    public final V call() throws Exception {
        stopwatch.reset();
        try {
            V result = compute();
            stopwatch.stop();
            return result;
        } catch (Throwable th) {
            stopwatch.stop();
            if (th instanceof Exception ex) {
                throw ex;
            } else {
                throw new Exception(th);
            }
        }
    }

    protected abstract V compute() throws Exception;

    public void addListener(TrackingListener listener) {
        if (this.listener == null) {
            this.listener = listener;
            this.listener.updateValue(this.value);
            this.listener.updateMessage(this.message);
            this.listener.updateTitle(this.title);
            this.listener.updateProgress(this.workDone, this.maxWork);
        } else {
            throw new IllegalStateException("Listener already set");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String estimateTimeRemainingString() {
        return "About " + DurationUtil.format(estimateTimeRemaining()) + " remaining.";
    }

    public Duration estimateTimeRemaining() {
        if (maxWork == 0) {
            return Duration.ofDays(365);
        }
        double percentDone = workDone / maxWork;
        if (percentDone < 0.00001) {
            return Duration.ofDays(365);
        }
        //(TimeTaken / linesProcessed) * linesLeft = timeLeft
        double secondsDuration = duration().getSeconds();
        double secondsRemaining = secondsDuration / workDone * (maxWork - workDone);
        return Duration.ofSeconds((long) secondsRemaining);
    }

    public Duration duration() {
        return stopwatch.duration();
    }

    public void completedUnitOfWork() {
        workDone++;
    }

    public String durationString() {
        return stopwatch.durationString();
    }

    public Duration averageDurationForElement(int count) {
        return stopwatch.averageDurationForElement(count);
    }

    public String averageDurationForElementString(int count) {
        return stopwatch.averageDurationForElementString(count);
    }

    protected void updateValue(V result) {
        if (listener != null) {
            listener.updateValue(result);
        }
    }

    protected void updateMessage(String message) {
        if (message != null && this.message == null) {
            if (listener != null) {
                listener.updateMessage(message);
            }
        } else if (listener != null && !this.message.equals(message)) {
            listener.updateMessage(message);
        }
        this.message = message;
    }

    protected void updateTitle(String title) {
        this.title = title;
        if (listener != null) {
            listener.updateTitle(title);
        }
    }

    protected void addToTotalWork(long amountToAdd) {
        updateProgress(workDone, this.maxWork + amountToAdd);
    }

    protected void updateProgress(double workDone, double maxWork) {
        boolean update = false;

        if (this.maxWork != maxWork) {
            this.maxWork = maxWork;
            this.workDone = workDone;
            update = true;
        } else {
            double difference = workDone - this.workDone;
            double percentDifference = difference / maxWork;
            if (percentDifference > updateThreshold) {
                update = true;
                this.workDone = workDone;
            }
        }

        if (listener != null && update) {
            listener.updateProgress(workDone, maxWork);
        }
    }

    protected void updateProgress(long workDone, long maxWork) {
        updateProgress((double) workDone, (double) maxWork);
    }
}
