package concurrent_final_project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores online driver state and location snapshots.
 *
 * This class combines the usual volatile visibility exercise and
 * ConcurrentHashMap delegation exercise into one project object.
 */
public final class DriverRegistry {
    private final ConcurrentHashMap<String, DriverSnapshot> drivers = new ConcurrentHashMap<>();

    private volatile boolean acceptingOrders = true;

    public void seedDemoDrivers() {
        registerOrUpdate(new DriverSnapshot("D-GZ-01", "广州", true, 23.1291, 113.2644));
        registerOrUpdate(new DriverSnapshot("D-GZ-02", "广州", true, 23.1190, 113.3200));
        registerOrUpdate(new DriverSnapshot("D-SZ-01", "深圳", true, 22.5431, 114.0579));
        registerOrUpdate(new DriverSnapshot("D-FS-01", "佛山", true, 23.0215, 113.1214));
    }

    public void registerOrUpdate(DriverSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("driver snapshot must not be null");
        }
        drivers.put(snapshot.driverId, snapshot);
    }

    public void stopAcceptingOrders() {
        acceptingOrders = false;
    }

    public boolean isAcceptingOrders() {
        return acceptingOrders;
    }

    public DriverSnapshot findDriverFor(RideOrder order) {
        return drivers.values().stream()
                .filter(driver -> driver.online)
                .filter(driver -> driver.city.equals(order.city))
                .min(Comparator.comparing(driver -> driver.driverId))
                .orElse(null);
    }

    public List<DriverSnapshot> snapshot() {
        return new ArrayList<>(drivers.values());
    }

    public void printDrivers() {
        System.out.println("\n========== 在线司机快照 ==========");
        snapshot().stream()
                .sorted(Comparator.comparing(d -> d.driverId))
                .forEach(driver -> System.out.println(driver));
    }

    public static final class DriverSnapshot {
        public final String driverId;
        public final String city;
        public final boolean online;
        public final double latitude;
        public final double longitude;
        public final long updatedAtMillis;

        public DriverSnapshot(String driverId, String city, boolean online, double latitude, double longitude) {
            this.driverId = driverId;
            this.city = city;
            this.online = online;
            this.latitude = latitude;
            this.longitude = longitude;
            this.updatedAtMillis = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format(
                    "%s city=%s online=%s location=(%.4f, %.4f)",
                    driverId,
                    city,
                    online,
                    latitude,
                    longitude);
        }
    }
}
