# esc-test

Provides tests for all event store commons implementations.

## Performance

**No profiling was done yet** - So the below results only give a rough overview about the current state.

| Implementation                                                                   | Type                | One append (100,000 at once) | Multiple appends (100,000 times) | Read chunks of 4,000 (100,000 events) | Read one by one (100,000 events) | 
|:---------------------------------------------------------------------------------|:--------------------|:-----------------------------|:---------------------------------|:--------------------------------------|:---------------------------------|
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Physical PC SSD (1) | 2,467 events / second        | 96 events / second               | 901 events / second                   | 464 events / second              |
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Physical PC HDD (2) | 2,426 events / second        | 15 events / second               | 909 events / second                   | 459 events / second              |
| [esc-eshttp](src/test/java/org/fuin/esc/test/performance/EsHttpPerformance.java) | Virtual Machine (3) | 4,042 events / second        | 379 events / second              | 832 events / second                   | 403 events / second              |

* 64 Bit Java 8 Runtime
* (1) Intel Core i7-3820 @ 3.60 GHz, 16 GB RAM, Windows 7 SP 1, Crucial M4 512GB SSD
* (2) Intel Core i7-3820 @ 3.60 GHz, 16 GB RAM, Windows 7 SP 1, WD Scorpio Blue WD10JPVT 1TB 5400 RPM 8MB Cache SATA
* (3) VMWare Workstation 12 + Ubuntu 14.04 Desktop (Same machine as (1))

_The slower performance on a physical machine is somewhat surprising... Should be investigated!_
