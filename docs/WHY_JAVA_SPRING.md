# Why Java + Spring Boot for the settlement service

CrossDesk's contracts live in Daml on Canton; the service tier that drives them
(`backend/`) is **Java 17 on Spring Boot 3.3**, talking to the **Canton Ledger API
over gRPC** via the **Daml Java Bindings 2.9.4**. That's a deliberate choice, not a
default — here's the reasoning, tied to what a settlement service actually has to do.

## The workload: a long-running service in front of a ledger

A settlement desk is not a batch job. It's a process that stays up, holds an
authenticated connection to a participant node, and turns a steady stream of REST
calls into Ledger API commands (`create`, `exercise`, transaction streaming). The
properties that matter are **steady-state throughput, predictable latency under
load, connection lifecycle management, and provable correctness of the command
mapping.** Java + Spring is a strong fit for each.

## Why the JVM

- **JIT → near-native steady-state throughput.** The hot paths here — marshalling
  a request into a gRPC command, decoding an active-contract-set snapshot — run
  millions of times over a process's life. HotSpot's C2 JIT profiles those paths
  and compiles them to optimized machine code after warmup, so a service that
  stays up settles at close to native speed rather than paying an interpreter tax
  on every call. For a start-once/run-forever settlement process, warmup is a
  one-time cost and the steady state is what counts.
- **Mature, tunable GC and observability.** G1/ZGC give predictable pause behavior
  under sustained load, and the JVM's introspection (JFR, thread/heap dumps, JMX)
  is exactly the production-debuggability an institutional operator expects from a
  service on the money path.
- **First-class gRPC + TLS.** The Ledger API is gRPC with mutually-authenticated
  mTLS and JWT-scoped `actAs`/`readAs`. `grpc-netty` (pulled transitively by the
  Daml bindings) is a first-class, battle-tested transport for exactly that — no
  hand-rolled HTTP/2.
- **The Daml bindings are Java-native.** `daml codegen java` emits strongly-typed
  template/choice classes, so a malformed command is a **compile error**, not a
  runtime surprise. The generated code is committed, so the Gradle/Docker build
  needs no Daml SDK on the box.

## Why Spring Boot specifically

- **Config binding → one jar, many ledgers.** `application.yml` is fully
  env-overridable (Spring relaxed binding), so the **same artifact** points at a
  local plaintext sandbox (`localhost:6865`, no auth) or a real Canton participant
  (`LEDGER_TLS=true` + `LEDGER_JWT=<bearer>`) with **zero code change** — see
  `LedgerProperties` / `application.yml`. That's what you want for something that
  has to promote cleanly from dev to a real participant.
- **Dependency injection for lifecycle.** The single `DamlLedgerClient` and its
  gRPC channel are owned by one `@Component` (`LedgerConnection`), connected
  **lazily** and closed on `@PreDestroy`, so the pod can start before the
  participant is reachable (Kubernetes-friendly) and a failed connect surfaces
  cleanly on the request that triggered it. DI makes that lifecycle explicit and
  testable instead of static and global.
- **Clean REST surface + validation.** `spring-boot-starter-web` +
  `-validation` give the controller layer (`SettlementController`) request
  validation and JSON error bodies for free — the boundary between "REST in" and
  "Ledger API command out" stays thin and readable.
- **TDD without a ledger.** `spring-boot-starter-test` (JUnit 5 + MockMvc) lets the
  pure command-mapping (`LedgerCommandsTest`) and the web slice
  (`SettlementControllerTest`) be tested with **no ledger running**; a
  `@Tag("integration")` end-to-end test runs against a live sandbox and is excluded
  from the default build. Fast feedback on the logic that turns an HTTP call into a
  settlement.

## The dependency list (small on purpose)

| Dependency | Why it's here |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded server, Jackson JSON |
| `spring-boot-starter-validation` | request-body validation at the boundary |
| `com.daml:bindings-java:2.9.4` | typed Ledger API client + codegen runtime |
| `com.daml:bindings-rxjava:2.9.4` | reactive Ledger API (transitively brings `grpc-netty`, protobuf, rxjava2 — the whole gRPC transport) |
| `spring-boot-starter-test` | JUnit 5 + MockMvc for the no-ledger unit/web tests |

Four runtime dependencies. The gRPC transport, protobuf, and reactive client all
arrive transitively through the two Daml bindings, so the settlement path is fully
wired with a deliberately small surface.

## In one line

The JVM gives a start-once/run-forever settlement service near-native steady-state
throughput and real production observability; Spring Boot gives it a clean,
testable REST→Ledger-API seam and one artifact that runs identically against a
sandbox or a live Canton participant. That combination — Java + Spring Boot + TDD
in front of an atomic settlement engine — is the same institutional stack these
systems are built on in production.
