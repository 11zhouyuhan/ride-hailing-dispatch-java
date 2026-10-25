package concurrent_final_project;

/**
 * One immutable ride order used throughout the final project.
 *
 * Students should not add setters here. The whole dispatch pipeline shares
 * RideOrder instances between threads, so immutability keeps the data safe.
 */
public final class RideOrder {
    public final int orderId;
    public final String passengerId;
    public final String city;
    public final String origin;
    public final String destination;
    public final double distanceKm;
    public final long createdAtMillis;
    public final boolean poisonPill;

    private RideOrder(
            int orderId,
            String passengerId,
            String city,
            String origin,
            String destination,
            double distanceKm,
            long createdAtMillis,
            boolean poisonPill) {
        this.orderId = orderId;
        this.passengerId = passengerId;
        this.city = city;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.createdAtMillis = createdAtMillis;
        this.poisonPill = poisonPill;
    }

    public static RideOrder create(
            int orderId,
            String passengerId,
            String city,
            String origin,
            String destination,
            double distanceKm) {
        return new RideOrder(
                orderId,
                passengerId,
                city,
                origin,
                destination,
                distanceKm,
                System.currentTimeMillis(),
                false);
    }

    public static RideOrder poisonPill() {
        return new RideOrder(-1, "SYSTEM", "-", "-", "-", 0, System.currentTimeMillis(), true);
    }

    @Override
    public String toString() {
        if (poisonPill) {
            return "POISON_PILL";
        }
        return String.format(
                "Order#%03d %s %s -> %s %.1fkm",
                orderId,
                city,
                origin,
                destination,
                distanceKm);
    }
}
