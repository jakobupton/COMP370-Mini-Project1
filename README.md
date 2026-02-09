# COMP370-Mini-Project1

## High Level Description

Your SRMS must simulate a small cluster of servers that provide a simple service (for example:
accept and respond to a basic ‘PING‘ or ‘PROCESS‘ request). Features required:
1. **Multiple server processes**: Implement at least three independent server processes (Java
programs) that listen on different ports and can be started/stopped independently.
2. **Primary–Backup architecture**: At any time one server is the *Primary* (handles client
requests), others are *Backups* (standby). Backup servers monitor the primary via heartbeats.
3. **Heartbeat / health checks**: Servers send periodic heartbeat messages to a monitor and/or
to each other. The monitor detects failure if heartbeats stop.
4. **Failover (automatic)**: When the primary fails, a backup detects the failure and performs
failover to become primary. The failover must avoid split-brain where possible (describe and
handle simple split-brain avoidance).
5. **Client interface**: A client program that sends requests (e.g., ‘PROCESS job-id‘) to the
primary. If the primary is down, the client should be able to reconnect to the new primary
(discoverable via the monitor).
6. **Logging and monitoring**: Servers and monitor log events with timestamps. Include a
simple console or file log describing state changes and failovers.
7. **Simulated failure scenarios**: Provide scripts or commands to simulate failures (kill process,
freeze process), network delays, or partitions (can be simulated with the server ignoring
heartbeats).

Run monitor:
```bash
java -cp out comp370.srms.ServerMonitor
```

Run server processes (new terminals):
```bash
java -cp out comp370.srms.ServerProcess localhost 3000 1000
java -cp out comp370.srms.ServerProcess localhost 3000 1000
java -cp out comp370.srms.ServerProcess localhost 3000 1000
```


## Testing and Failure Scenarios
| Scenario | Description and Tasks |
|---|---|
| Normal operation | Start the monitor and 3 server instances. Confirm that a primary server is correctly selected. Start client(s) and send requests to the system. Verify clients receive correct responses and that the server state is properly replicated to backups. |
| Primary crash | Forcefully stop (kill) the primary server process. Check that the monitor detects this failure promptly and promotes a backup server to primary. Verify that clients reconnect automatically and continue to have their requests served without errors. |
| Backup crash | Kill one of the backup servers during operation. Verify the primary server continues serving client requests without disruption. Restart the killed backup server and confirm it synchronizes its state correctly with the primary before resuming normal operation. |
| Simultaneous failures | Kill the primary server and one backup server at the same time. Observe if the cluster still successfully elects a new primary server, if possible. Document any system limits encountered (e.g., if all servers fail, what happens?). |
| Network delay simulation | Introduce artificial delays in heartbeat messages or their handling (for example, by adding sleep statements in the code). Verify that your failure detection thresholds are tuned to avoid false failover triggers caused by delays. |
| Recovery | Restart all crashed servers. Observe the process of servers rejoining the cluster, synchronizing their state, and being assigned correct roles (primary or backup). |


## Contributors
- Jakob Upton - https://github.com/jakobupton
- Jakob Hobek - https://github.com/EGGBEING
- Massimo Currier - https://github.com/massiimo22
- Donald Okonkwo - https://github.com/donzyC
- Gavin McNaughton - https://github.com/Alashir

## References

## Link to Repository 
https://github.com/jakobupton/COMP370-Mini-Project1
