package concurrent_final_project;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transfers fare from passenger account to driver account.
 */
public final class PaymentService {
    private final Account passengerAccount = new Account(101, "乘客账户", 10000.0);
    private final Account driverAccount = new Account(201, "司机账户", 1000.0);
    private final double initialTotal = passengerAccount.balance() + driverAccount.balance();
    private final AtomicInteger successfulTransfers = new AtomicInteger();

    public boolean payDriver(double amount) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("payment interrupted before transfer");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }

        Account first = passengerAccount.id < driverAccount.id ? passengerAccount : driverAccount;
        Account second = passengerAccount.id < driverAccount.id ? driverAccount : passengerAccount;

        synchronized (first) {
            synchronized (second) {
                if (passengerAccount.balance < amount) {
                    return false;
                }
                passengerAccount.balance -= amount;
                driverAccount.balance += amount;
                successfulTransfers.incrementAndGet();
                return true;
            }
        }
    }

    public boolean balanceConserved() {
        double currentTotal = passengerAccount.balance() + driverAccount.balance();
        return Math.abs(currentTotal - initialTotal) < 0.001;
    }

    public int successfulTransfers() {
        return successfulTransfers.get();
    }

    public void printSummary() {
        System.out.println("\n========== 支付汇总 ==========");
        System.out.printf("%s%n", passengerAccount);
        System.out.printf("%s%n", driverAccount);
        System.out.printf("成功转账次数: %d%n", successfulTransfers());
        System.out.printf("余额守恒: %s%n", balanceConserved() ? "PASS" : "FAIL");
    }

    private static final class Account {
        final int id;
        final String name;
        private double balance;

        Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        synchronized double balance() {
            return balance;
        }

        synchronized boolean withdraw(double amount) {
            if (balance < amount) {
                return false;
            }
            balance -= amount;
            return true;
        }

        synchronized void deposit(double amount) {
            balance += amount;
        }

        @Override
        public synchronized String toString() {
            return String.format("%s(id=%d, balance=%.2f)", name, id, balance);
        }
    }
}
