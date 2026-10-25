package concurrent_final_project;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified final-project entry point.
 *
 * Students should run this one class for the final screenshot:
 *
 * javac -encoding UTF-8 -d out src/concurrent_final_project/*.java
 * java -cp out concurrent_final_project.RideHailPlatform
 */
public final class RideHailPlatform {
    public static void main(String[] args) throws Exception {
        System.out.println("========== 并行开发编程期末项目：高并发网约车订单调度平台 ==========");

        OrderMetrics metrics = new OrderMetrics();
        DriverRegistry drivers = new DriverRegistry();
        PriceQueryService prices = new PriceQueryService();
        PaymentService payments = new PaymentService();
        MonitoredDispatchPool pool = new MonitoredDispatchPool();

        drivers.seedDemoDrivers();
        drivers.printDrivers();

        DispatchCenter center = new DispatchCenter(pool, drivers, prices, payments, metrics, 3);
        center.start();

        for (RideOrder order : demoOrders()) {
            center.submit(order);
        }

        center.shutdownGracefully();

        metrics.printSummary();
        payments.printSummary();
        printAcceptanceReport(metrics, payments, pool, center);
    }

    private static List<RideOrder> demoOrders() {
        List<RideOrder> orders = new ArrayList<>();
        orders.add(RideOrder.create(1, "P001", "广州", "广州南站", "珠江新城", 18.2));
        orders.add(RideOrder.create(2, "P002", "广州", "天河客运站", "琶洲会展中心", 14.1));
        orders.add(RideOrder.create(3, "P003", "深圳", "深圳北站", "南山科技园", 16.0));
        orders.add(RideOrder.create(4, "P004", "佛山", "佛山祖庙", "千灯湖", 9.5));
        orders.add(RideOrder.create(5, "P005", "广州", "白云机场", "体育西路", 31.2));
        orders.add(RideOrder.create(6, "P006", "深圳", "福田口岸", "宝安中心", 24.8));
        orders.add(RideOrder.create(7, "P007", "广州", "广州东站", "大学城北", 22.6));
        orders.add(RideOrder.create(8, "P008", "佛山", "岭南天地", "广州南站", 19.9));
        orders.add(RideOrder.create(9, "P009", "广州", "越秀公园", "猎德", 8.1));
        orders.add(RideOrder.create(10, "P010", "深圳", "世界之窗", "深圳湾口岸", 6.3));
        orders.add(RideOrder.create(11, "P011", "广州", "黄埔大道", "北京路", 12.0));
        orders.add(RideOrder.create(12, "P012", "佛山", "顺德大良", "佛山西站", 28.4));
        return orders;
    }

    private static void printAcceptanceReport(
            OrderMetrics metrics,
            PaymentService payments,
            MonitoredDispatchPool pool,
            DispatchCenter center) {
        System.out.println("\n========== 验收报告 ==========");
        boolean countOk = metrics.submittedOrders() == 12 && metrics.dispatchedOrders() == 12;
        boolean queueOk = center.queuedOrders() == 0;
        boolean paymentOk = payments.balanceConserved() && payments.successfulTransfers() == 12;
        boolean poolOk = pool.completedDispatchTasks() >= 3;

        System.out.printf("[CHECK-1] 12 笔订单全部完成: %s%n", countOk ? "PASS" : "FAIL");
        System.out.printf("[CHECK-2] 停机后队列清空: %s%n", queueOk ? "PASS" : "FAIL");
        System.out.printf("[CHECK-3] 支付余额守恒且 12 次转账: %s%n", paymentOk ? "PASS" : "FAIL");
        System.out.printf("[CHECK-4] 线程池监控记录有效: %s%n", poolOk ? "PASS" : "FAIL");
        System.out.printf("[CHECK-5] 未使用 Thread.stop/suspend/resume: 人工检查%n");
    }
}
