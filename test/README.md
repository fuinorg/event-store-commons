# esc-test
Provides tests for all event store commons implementations.

## Performance

The below results should give a rough overview about the performance of the different implementations.

| Implementation | Type              | One append (100,000 at once) | Multiple appends (100,000 times) | 
|:---------------|:------------------|:-----------------------------|:---------------------------------|
| esc-eshttp     | Physical PC*      | 4,430 events / second        | 369 events / second              |
| esc-eshttp     | Virtual Machine** | ?     events / second        | ?   events / second              |

* Intel Core i7-3820 @ 3.60 GHz, 16 GB RAM, Windows 7 SP 1, Crucial M4 512GB SSD
** VMWare Workstation 12 + Ubuntu 14.04 Desktop
