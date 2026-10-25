package concurrent_final_project;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ThreadPoolExecutor with timing hooks and CallerRunsPolicy.
 */
public final class MonitoredDispatchPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicLong totalMillis = new AtomicLong();

    public MonitoredDispatchPool() {
        super(
                3,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3),
                new NamedThreadFactory("dispatch-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable task) {
        super.beforeExecute(thread, task);
        startTime.set(System.currentTimeMillis());
        System.out.printf("[POOL] %s start task%n", thread.getName());
    }

    @Override
    protected void afterExecute(Runnable task, Throwable throwable) {
        super.afterExecute(task, throwable);
        try {
            Long startedAt = startTime.get();
            long elapsed = startedAt == null ? 0L : System.currentTimeMillis() - startedAt;
            int done = completed.incrementAndGet();
            totalMillis.addAndGet(elapsed);
            if (throwable != null) {
                System.out.printf("[POOL-ERROR] task failed: %s%n", throwable);
            }
            System.out.printf("[POOL] task finished elapsed=%dms completed=%d avg=%.2fms%n",
                    elapsed,
                    done,
                    averageMillis());
        } finally {
            startTime.remove();
        }
    }

    public int completedDispatchTasks() {
        return completed.get();
    }

    public double averageMillis() {
        int count = completed.get();
        return count == 0 ? 0 : (double) totalMillis.get() / count;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger next = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, prefix + "-" + next.getAndIncrement());
        }
    }
}
