# work-management-proxy — J17 → J25 behavioural parity findings

Part of the work-management-proxy-combo parity audit (PEG-3402), per the CTP parity guide
(Confluence 1990371020) and the users-groups reference (PEG-3336). J17 (`main`) is the source of truth.

## Context shape

Camunda work-management proxy: an access-control API over the workflow engine. **No JPA persistence
of its own** (0 `@Entity`, no `EntityManager`), **no production Java source change J17→J25** beyond
`javax`→`jakarta` imports, and the golden test JSON is unchanged.

## BC catalogue disposition

| BC | Present? | Disposition |
|----|----------|-------------|
| BC-01/02/04/05/06 | No | N/A — no JPA / no EntityManager |
| BC-07 | No | N/A — no `liquibase.hub.mode` |
| BC-11 | No | N/A |
| BC-20 | **Yes** (1 kbase) | **Guarded** — `AccessControlRuleCountTest` for kbase `WorkManagement.Proxy.API`. Both branches. |
| BC-24 | Runtime | Covered by ITs |

## Change

- **BC-20:** `work-management-proxy-api/.../rule/AccessControlRuleCountTest.java` — rule-count guard
  for kbase `WorkManagement.Proxy.API`. Both branches.
