## UML Review and Analysis

### Original Design Limitations

After reviewing the Mini Project 1 UML diagrams and implementation, the following design limitations were identified:

1. **Monitor Singleton Missing**
   - The ServerMonitor class had a private constructor but no mechanism preventing multiple instances
   - Multiple monitors could theoretically exist, leading to split-brain scenarios
   - **Impact**: Inconsistent primary server tracking across multiple monitor instances

2. **Observer Pattern Incomplete**
   - While Observer interface existed, notification logic was manually coded
   - Logger was called directly from Monitor, not through Observer interface
   - **Impact**: Tight coupling between Monitor and logging/admin components

3. **Abstraction-Occurrence Already Implemented**
   - ServerType class correctly separates server configuration from instances
   - **Status**: No changes required

### Pattern Application Rationale
| Pattern | Applied To | Rationale | Benefits |
|---------|-----------|-----------|----------|
| **Singleton** | ServerMonitor | Ensure exactly one monitor instance exists as the single source of truth | Prevents split-brain, centralizes failover authority |
| **Observer** | Monitor notifications | Decouple Monitor from logging/admin components | Easier to add new observers (metrics, alerts) without modifying Monitor |
| **Abstraction-Occurrence** | Server hierarchy | Already implemented via ServerType class | Separates server type from instances, reduces duplication |

### Updated UML Changes

#### 1. Class Diagram Changes

| Change | Description |
|--------|-------------|
| `<<singleton>>` stereotype | Added to Monitor class to indicate Singleton pattern |
| `-static instance` | Private static instance field for Singleton |
| `+getInstance()` | Public static method to access Singleton instance |
| `LoggerObserver` class | New class implementing Observer interface |
| `+notifyObservers()` | New method in Monitor for generic observer notification |

#### 2. Sequence Diagram Changes

| Diagram | Changes Made |
|---------|--------------|
| Startup & Election | Added observer notification steps after primary election |
| Failover | Added observer notification steps during failover event |
| Both Diagrams | Added LoggerObserver and AdminConnection receiving updates |

### Before vs After Comparison

| Aspect | Before (MP1) | After (MP2) |
|--------|--------------|-------------|
| **Monitor instances** | Could create multiple instances | Singleton guarantees single instance |
| **Notification logic** | Direct method calls to specific components | Observer pattern with loose coupling |
| **Adding new observers** | Modify Monitor code for each new observer | Implement Observer interface only, no Monitor changes |
| **Code duplication** | Logger calls scattered across methods | Centralized in `notifyObservers()` method |
| **Testability** | Hard to mock multiple observers | Easy to mock Observer interface |
| **Extensibility** | Adding metrics required Monitor changes | Add new Observer without touching Monitor |