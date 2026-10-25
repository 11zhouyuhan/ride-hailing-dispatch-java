# High-Concurrency Ride-Hailing Order Dispatch Platform

A Java concurrency project that simulates a ride-hailing dispatch pipeline:

`submit order -> bounded blocking queue -> worker threads -> parallel price quotes -> driver match -> payment transfer -> metrics & graceful shutdown`

Final project for the *Concurrent Development* course (Nanfang College, Guangzhou, 2026). Team of 4.

> **Origin.** The course provided a code skeleton (project structure, demo data, acceptance report, queue/latch/shutdown scaffolding) with six `TODO` sections for students to complete. This repository contains the completed version written by our team.

## Pipeline

1. `RideHailPlatform` creates the shared services, registers demo drivers (Guangzhou, Shenzhen, Foshan) and submits 12 demo orders.
2. `DispatchCenter` starts 3 worker threads in a custom thread pool; all workers wait on a `CountDownLatch` start gate.
3. `submit()` puts orders into an `ArrayBlockingQueue` (capacity 8); the producer blocks when the queue is full, so no orders are dropped.
4. Each worker takes an order, gets the cheapest quote from three providers in parallel (2.5 s timeout), matches an online driver in the same city, and transfers payment.
5. Shutdown puts one poison pill per worker on the queue, so every worker exits cleanly, then waits for the pool to terminate.

## Components and concurrency techniques

| Component | Technique |
|---|---|
| `OrderMetrics` | `AtomicLong`, `ConcurrentHashMap.computeIfAbsent` for per-city counters |
| `DriverRegistry` | immutable `DriverSnapshot` in a `ConcurrentHashMap`; `volatile` stop flag |
| `PriceQueryService` | fixed thread pool, `ExecutorCompletionService`, deadline-based timeout, cancel unfinished futures |
| `PaymentService` | accounts locked in ascending id order to avoid deadlock; balance conservation check |
| `MonitoredDispatchPool` | `ThreadPoolExecutor` with `beforeExecute`/`afterExecute` timing via `ThreadLocal`, `CallerRunsPolicy` |
| `DispatchCenter` | blocking queue, start gate, poison-pill shutdown, per-order error handling |

## My contribution

I implemented the `DispatchCenter` worker pipeline (blocking `submit`, the worker loop: take an order, exit on poison pill, quote, driver match, payment, record and log), ran the end-to-end acceptance checks, and integrated the team's documentation. The other modules were written by teammates.

## Run

Requires JDK 11+.

```bash
javac -encoding UTF-8 -d out src/concurrent_final_project/*.java
java -cp out concurrent_final_project.RideHailPlatform
```

The program ends with an acceptance report (all 12 orders dispatched, queue drained, balances conserved over 12 transfers, pool monitoring recorded).
