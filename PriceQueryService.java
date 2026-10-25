package concurrent_final_project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Parallel quote service for a single ride order.
 */
public final class PriceQueryService {
    public Quote bestQuote(RideOrder order, long timeoutMillis) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CompletionService<Quote> completionService = new ExecutorCompletionService<>(executor);
        List<Future<Quote>> futures = new ArrayList<>();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        Quote best = null;

        try {
            for (Callable<Quote> task : providerTasks(order)) {
                futures.add(completionService.submit(task));
            }
            for (int i = 0; i < futures.size(); i++) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }
                Future<Quote> finished = completionService.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (finished == null) {
                    break;
                }

                try {
                    Quote quote = finished.get();
                    if (quote != null && (best == null || quote.price < best.price)) {
                        best = quote;
                    }
                } catch (ExecutionException e) {
                    System.out.printf("[PRICE-ERROR] order=%s cause=%s%n", order, e.getCause());
                }
            }

            return best;
        } finally {
            for (Future<Quote> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
            executor.shutdownNow();
        }
    }

    private List<Callable<Quote>> providerTasks(RideOrder order) {
        List<Callable<Quote>> tasks = new ArrayList<>();
        tasks.add(() -> queryProvider("岭南出行", order, 800, 2.6, 8.0));
        tasks.add(() -> queryProvider("湾区快车", order, 1200, 2.4, 9.0));
        tasks.add(() -> queryProvider("城际专车", order, 1800, 2.2, 10.0));
        return tasks;
    }

    private Quote queryProvider(String provider, RideOrder order, long delayMillis, double kmRate, double baseFare)
            throws InterruptedException {
        Thread.sleep(delayMillis);
        double price = baseFare + order.distanceKm * kmRate;
        return new Quote(provider, price, delayMillis);
    }

    public static final class Quote {
        public final String provider;
        public final double price;
        public final long responseMillis;

        public Quote(String provider, double price, long responseMillis) {
            this.provider = provider;
            this.price = price;
            this.responseMillis = responseMillis;
        }

        @Override
        public String toString() {
            return String.format("%s ¥%.2f (%dms)", provider, price, responseMillis);
        }
    }
}
