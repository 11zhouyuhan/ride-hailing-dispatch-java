package concurrent_final_project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe counters for the whole dispatch project.
 *
 * TODO-1 is intentionally small: students only need to make counters correct
 * under concurrent calls. The final acceptance report reads these counters.
 */
public final class OrderMetrics {
    private final AtomicLong submittedOrders = new AtomicLong();
    private final AtomicLong dispatchedOrders = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> dispatchedByCity = new ConcurrentHashMap<>();

    public void recordSubmitted(RideOrder order) {
        if (!order.poisonPill) {
            submittedOrders.incrementAndGet();
        }
    }

    public void recordDispatched(RideOrder order) {
        if (order.poisonPill) {
            return;
        }

        dispatchedOrders.incrementAndGet();
        dispatchedByCity
                .computeIfAbsent(order.city, key -> new AtomicLong())
                .incrementAndGet();
    }

    public long submittedOrders() {
        return submittedOrders.get();
    }

    public long dispatchedOrders() {
        return dispatchedOrders.get();
    }

    public long dispatchedInCity(String city) {
        AtomicLong value = dispatchedByCity.get(city);
        return value == null ? 0 : value.get();
    }

    public void printSummary() {
        System.out.println("\n========== 订单统计 ==========");
        System.out.printf("提交订单数: %d%n", submittedOrders());
        System.out.printf("完成订单数: %d%n", dispatchedOrders());
        for (Map.Entry<String, AtomicLong> entry : dispatchedByCity.entrySet()) {
            System.out.printf("城市 %-4s: %d 单%n", entry.getKey(), entry.getValue().get());
        }
    }
}
