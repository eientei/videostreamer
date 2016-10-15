package org.eientei.videostreamer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class NamedThreadFactory implements ThreadFactory {
    private final Logger log = LoggerFactory.getLogger(NamedThreadFactory.class);
    private final String prefix;
    private final AtomicInteger sequence = new AtomicInteger(0);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, prefix + "-" + sequence.incrementAndGet());
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(final Thread thread, final Throwable e) {
                log.error(thread +  " produced exception", e);
            }
        });
        return thread;
    }
}
