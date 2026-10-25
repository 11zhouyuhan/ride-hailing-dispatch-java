package concurrent_final_project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The unified dispatch pipeline.
 *
 * Order lifecycle:
 * submit -> queue -> worker -> quote -> driver match -> payment -> metrics.
 */
public final class DispatchCenter {
    private final ArrayBlockingQueue<RideOrder> queue = new ArrayBlockingQueue<>(8);
    private final CountDownLatch startGate = new CountDownLatch(1);
    private final List<Runnable> workers = new ArrayList<>();
    private final MonitoredDispatchPool pool;
    private final DriverRegistry drivers;
    private final PriceQueryService prices;
    private final PaymentService payments;
    private final OrderMetrics metrics;
    private final int workerCount;

    public DispatchCenter(
            MonitoredDispatchPool pool,
            DriverRegistry drivers,
            PriceQueryService prices,
            PaymentService payments,
            OrderMetrics metrics,
            int workerCount) {
        this.pool = pool;
        this.drivers = drivers;
        this.prices = prices;
        this.payments = payments;
        this.metrics = metrics;
        this.workerCount = workerCount;
    }

    public void start() {
        for (int i = 1; i <= workerCount; i++) {
            Runnable worker = workerLoop("W" + i);
            workers.add(worker);
            pool.submit(worker);
        }
        startGate.countDown();
    }

    public void submit(RideOrder order) throws InterruptedException {
        metrics.recordSubmitted(order);
        queue.put(order);
        System.out.printf("[SUBMIT] %s%n", order);
    }

    public void shutdownGracefully() throws InterruptedException {
        drivers.stopAcceptingOrders();
        for (int i = 0; i < workerCount; i++) {
            queue.put(RideOrder.poisonPill());
        }
        pool.shutdown();
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            List<Runnable> dropped = pool.shutdownNow();
            System.out.printf("[SHUTDOWN] force shutdown, dropped tasks=%d%n", dropped.size());
        }
    }

    public int queuedOrders() {
        return queue.size();
    }

    private Runnable workerLoop(String workerId) {
        return () -> {
            try {
                startGate.await();
                while (true) {
                    RideOrder order = queue.take();
                    if (order.poisonPill) {
                        System.out.printf("[%s] receive POISON_PILL, shutdown worker%n", workerId);
                        break;
                    }

                    try {
                        PriceQueryService.Quote quote = prices.bestQuote(order, 2500);
                        if (quote == null) {
                            System.out.printf("[%s] dispatch failed: no quote for %s%n", workerId, order);
                            continue;
                        }

                        DriverRegistry.DriverSnapshot driver = drivers.findDriverFor(order);
                        if (driver == null) {
                            System.out.printf("[%s] dispatch failed: no online driver for %s%n", workerId, order);
                            continue;
                        }

                        boolean paid = payments.payDriver(quote.price);
                        if (!paid) {
                            System.out.printf("[%s] dispatch failed: insufficient balance for %s price=%.2f%n",
                                    workerId,
                                    order,
                                    quote.price);
                            continue;
                        }

                        metrics.recordDispatched(order);
                        System.out.printf("[%s] DISPATCHED %s driver=%s quote=%s%n",
                                workerId,
                                order,
                                driver.driverId,
                                quote);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        System.out.printf("[%s] dispatch exception for %s: %s%n", workerId, order, e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] interrupted%n", workerId);
            }
        };
    }
}
