## Maintainability
The pattern-based version has led to a mixed bag of results. In terms of maintainability, the new version has clearer boundaries.
There is now a singleton clearly used within `ServerMonitor.java`, along with metadata for the server grouped in `ServerType.java`. We have also
split out some observer-related responsibilities into `Observer.java` & `AdminConnection.java`. This meant better code organization overall. Some issues are that the singleton adds global state, and required us to tweak
our tests for resetting the state between tests.

## Extensibility
The pattern-based version is better for a few reasons in terms of extensibility - The original version was a little more direct, but also more rigid, making it less flexible for additions.
The admin behaviour relied on polling, and server state were loose fields within `ServerRecord`, while the refactor created points to extend.
`ServerType` made it easier to add new data about a server or its behaviour without touching every `ServerRecord`. The observer pattern now also would allow multiple admin listeners or new types of update consumers, which was not
an option in the prior implementation. Our `ServerMonitor` currently still stores an `AdminConnection` instead of an `Observer` which means we are not fully using the abstraction yet, limiting the full benefits of it.

## Complexity
However, the pattern-based version is certainly more complex. The original code was more straightforward, with less moving parts, less state to manage, and fewer interactions between our threads & sockets. The refactor added a singleton lifecycle, registration of observers, sockets for callbacks, and persistent connections for server monitors. This complexity is not necessarily bad, as some of it backs new features, but it does mean that there are overall more moving parts.
For example, the observer flow and reconnect logic increase complexity but for good reason.

## Overall
Overall, the original was simpler and I believe simple software is good software. If someone on my team is putting a PR for review, or if I have to work in some unfamiliar part of the codebase, simplicity is king. It makes it much easier to get a quick grasp on how the system works, and lends itself to easier modification / bug auditing. At the end of the day, everything in software is choosing between various trade-offs. If the goal is lowest operational and cognitive complexity, then the original version is stronger. However, if the goal is clean software, then using these patterns effectively can result in more maintainable software. 