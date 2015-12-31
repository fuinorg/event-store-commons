# esc-test
Provides tests for all event store commons implementations.

## Performance

**No profiling was done yet** - So the below results only give a rough overview about the current state.

| Implementation                                                                   | Type                | One append (100,000 at once) | Multiple appends (100,000 times) | 
|:---------------------------------------------------------------------------------|:--------------------|:-----------------------------|:---------------------------------|
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Physical PC SSD (1) | 2,704 events / second        | 100 events / second              |
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Physical PC HDD (2) | 2,680 events / second        | _Event Store crashed_            |
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Virtual Machine (3) | 4,430 events / second        | 369 events / second              |

* 64 Bit Java 8 Runtime
* (1) Intel Core i7-3820 @ 3.60 GHz, 16 GB RAM, Windows 7 SP 1, Crucial M4 512GB SSD
* (2) Intel Core i7-3820 @ 3.60 GHz, 16 GB RAM, Windows 7 SP 1, WD Scorpio Blue WD10JPVT 1TB 5400 RPM 8MB Cache SATA 
* (3) VMWare Workstation 12 + Ubuntu 14.04 Desktop (Same machine as (1))

_The slower performance on a physical machine is somewhat surprising... Should be investigated!_
