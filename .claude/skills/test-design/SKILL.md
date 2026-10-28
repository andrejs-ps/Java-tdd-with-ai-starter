---
name: test-design
description: Test design techniques. Use when analyzing requirements to derive test cases, or when practicing TDD and writing automated tests.
---

# EP and BVA

- **EP & BVA:** split the input into partitions that should behave the same (include invalid ones), cover each partition, and test just around its edges, where bugs cluster.
- **Special business values:** 0 (division by zero, nullifies math), 1, -1 ("missing" marker), 0.01 (smallest currency unit), 100, 100.1, 101 (percent max and beyond), any domain-specific value.
- **Special Java values:** `Integer.MIN_VALUE`/`MAX_VALUE` and ±1 (overflow), `Long.MIN_VALUE`/`MAX_VALUE`, `Double.MAX_VALUE`/`MIN_VALUE`.
- **Time:** start/end of minute and hour, midnight, noon.
- **Dates:** start/end of day, month, year and other periods, leap day (29 Feb), always-invalid dates (30 Feb), daylight saving transitions, Unix epoch (1 Jan 1970) and earlier.
- **Strings:** empty, whitespace only, below min/above max length, accented and special characters, path length limits (255/260 on some OSes).

# CRUD

Test an entity across its whole lifecycle: created, read, updated, deleted.

## Create

- Created object reads back with the same data. E.g. create customer "Ann", get it by id, name is "Ann".
- Missing required field is rejected with a clear error. E.g. no email → 400 "email is required".
- Missing optional field is accepted. E.g. no phone → created.
- Duplicate key is rejected with a clear message that leaks nothing sensitive. E.g. "email already registered", not "user ann@x.com belongs to account 42".
- Allowed copies differ where expected. E.g. copied order gets a new id and a new created-at timestamp.
- Failure midway leaves no half-created object. E.g. order saved but its items fail → no order, or order and items both there.

## Read

- Reflects the latest create, update and delete. E.g. create then list: new item is in the list.
- Same data, sorting, position and formatting in every view (UI, API, search, export). E.g. same date format and currency in the list and in the export.
- Deleted or archived data is hidden. E.g. deleted customer not in the list or search.
- Cached data is refreshed after a change. E.g. rename, then read: the new name, not the cached old one.

## Update

- Saved change is visible on read. E.g. change price 10 → 12, get: 12.
- Effect on sorting and position. E.g. rename "Bob" to "Aaron" in a list sorted by name: it moves from last to first. Raise a price in a list sorted by price: it moves down.
- Effect on every view. E.g. renamed customer shows the new name in the list, search, detail, export and on their orders.
- Effect on related entities. E.g. change flight time: its bookings show the new time.
- No side effects on unrelated objects. E.g. update order 1: order 2 and the other fields of order 1 are unchanged.
- Key can't be changed to an existing key. E.g. set email to one already used → rejected.
- Updating a deleted object gives a clear error. E.g. update deleted order → 404 "order not found".
- Concurrent updates are resolved. E.g. two users set the price at once: last write wins, or the second gets a conflict error. Never a silent mix.

## Delete

- Deleted object is no longer readable, with a clear message. E.g. get deleted order → 404 "order not found".
- Second delete is handled gracefully. E.g. delete order 1 twice: no crash and no 500. The second returns 404 (or 204 if delete is idempotent), and nothing else is deleted.
- Cascade removes or invalidates dependents. E.g. delete customer: their orders are deleted, or the delete is refused while orders exist. No orphans.
- No side effects on other objects. E.g. delete order 1: order 2 and the customer remain.
- Lists and caches stop showing it. E.g. delete, then list: item gone, count is one lower, and sorting and positions of the rest are unchanged.

# Heuristics

## Assume Misuse

Users will use the system wrongly, creatively or maliciously. Expect it and test for it.

- Tampered URLs: change IDs, query parameters or path segments. E.g. GET /orders/2 as the owner of order 1 → 403/404, not order 2's data.
- Unrealistically long strings: rejected before they reach the backend or logs. E.g. 1 MB name → 400.
- Missing, duplicated, reordered or replayed requests. E.g. the same "pay" request sent twice → charged once.
- Skipped steps, refresh at a bad moment, Back button. E.g. open the confirm page directly without filling in the form → redirected, no order. Refresh after submit → no second order.
- Multiple tabs or sessions at once. E.g. same cart open in two tabs, check out in both → one order.
- Malformed, encoded or binary data pasted in. E.g. emoji, `%00`, `<script>`, a binary blob in a text field → rejected or stored safely, never a 500.
- Bypassed UI and client-side validation. E.g. call the API directly with a negative quantity the form would block → still rejected.

## Reverse & Redo

Do the actions in the opposite order, undo everything, or start at the end and work backward. Best for stateful systems (sessions, caches, workflows) and distributed systems.

- Look for incomplete cleanup. E.g. add item, remove it: no orphaned rows, no leaked connections or files.
- Look for state corruption from missing preconditions. E.g. cancel an order that was never paid → clear error, no refund created.
- Look for actions that should be reversible but aren't. E.g. archive then restore a customer: all data is back.
- Look for order-dependent bugs. E.g. ship then pay vs pay then ship: only the allowed order works, the other is refused.
- Look for wrong "can't happen anymore" assumptions. E.g. edit an order after it was cancelled → refused, not silently saved.
- Look for UI and backend mismatches. E.g. undo in the UI, reload: the backend shows the same state.

# ZOM - Zero, One, Many

**Zero** and **One** are edge cases, and **Many** is the first generalization - if the function or module works correctly with 2 "things", it is likely to work with 3, 4, and more (though far from always).

## Zero

For convenience and structure, "Zero" cases are grouped into in-memory and out-of-memory.

### In-Memory "Zero" Cases\*

* **Null / None** references (standalone variables, inside `List`, `Set`, or `Map/dict values`)
* **Numeric zero values**: `0`, `0.0`
* **NaN** (Not a Number)
* **Empty or blank string** (`""`, `" "`)
* **Empty collections**: `[]`, `{}`, empty `Set` or `Map`, empty Map values, empty JSON array/object
* **Default-constructed / uninitialized objects** (all fields at defaults)
* **Boolean false** (logical zero in some contexts)

*\*Although these originate in memory, zero values can propagate into or come back from external systems, and can occur in an out-of-memory context (see below).*

### Out-of-Memory (External) "Zero" Cases

* **Database query results**:
  * No rows returned (`SELECT ...` yields an empty result set)
  * Aggregations yielding `0` (e.g., `COUNT(*)=0`)
* **File system / I/O**:
  * Zero-length file
  * Empty stream or socket response
* **API / Service responses**:
  * Empty JSON/XML response body
  * Valid response with an empty list or object
  * `null` or missing fields in the payload
* **Message queues / event buses**:
  * No messages available (empty poll)

## One and Many

**One** is a frequent edge case: exactly one item in a cart, one transfer, one element in a `List` or `Set`. **Many**: "if it works with 2 things, it will work with more" is a good start but not always enough. Special, context-dependent counts matter, e.g. odd vs even.

E.g. total value of a portfolio: 0 positions → 0.00. 1 position → that position's value. 2 positions → the sum. Also try an odd count (3), and positions in different currencies or with a negative value.
