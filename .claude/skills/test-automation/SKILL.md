---
name: test-automation
description: Use to write automated tests following good patterns and avoiding test code smells
---

# Fix test code smells

A poorly built test suite becomes a maintenance burden. Avoid these test-specific anti-patterns.

## 1. Poor test name

Invest time in informative yet succinct names. Improve a poor name when you meet one.

- **Useless `test` prefix/suffix:** drop it when `@Test` is there. `testAdditionWorks` → `additionWorks`.
- **Name that lies:** the name claims one thing but the test does another, or does several checks. Unit tests test one thing. Rename and parameterize, or split.
- **Ambiguous:** you can't tell what is tested.

  ```java
  // before
  @Test
  void badPrice() {}

  // after
  @Test
  void priceAboveLimitRejected() {}
  ```

- **Inconsistent:** pick one style. `priceAboveLimitRejected`, `priceAtLimitAcceptedWithWarning`, `priceBelowLimitAccepted` (not a mix with `whenPriceBelowLimit_thenAccept`).

## 2. Poor test body

- **Production logic duplicated in the test:** the test gives input and checks output. Don't compute `int expected = 2 + 3;`, write `int expected = 5;`.
- **Non-parameterized:** 2+ tests that differ only by data should be one parameterized test.

  ```java
  // before - 3 tests
  @Test
  void rejectsTooShort() {
      assertFalse(validator.isValid("ab"));
  }

  @Test
  void rejectsWithDigit() {
      assertFalse(validator.isValid("abc1"));
  }

  @Test
  void rejectsWithSpecialChar() {
      assertFalse(validator.isValid("abc!"));
  }

  // after
  @ParameterizedTest
  @ValueSource(strings = { "ab", "abc1", "abc!" })
  void rejectsInvalidUsername(String input) {
      assertFalse(validator.isValid(input));
  }
  ```

- **Over-parameterized:** logically different scenarios merged into one test. Signs: input and expected output are both parameters, `if/else` in the test body, hard to say what it verifies.

- **Complex / large:** no loops or branching, mostly flat. Improve names, split, parameterize.

## 3. Assertions

- **No or commented-out assertions:** gives false confidence. 
- **Too many assertions:** if the first fails, the rest don't run. One test per behavior. Similar assertions → parameterize, different scenarios → split. A few fail-fast checks in integration tests are OK.
- **Wrong assertion:** pick the one with the most useful failure message. `assertEquals(5, x)` not `assertTrue(x == 5)`; 

## 4. Setup / cleanup

- **Interdependent tests:** each test must pass in any order. Each creates its own data and cleans it up. Slower, but stable.
- **Non-extracted setup:** self-contained tests are easier to read, but when many tests repeat the same setup/teardown, extract it.

  ```java
  // before - repeated in every test
  @Test
  void depositIncreasesBalance() {
      Account account = new Account("ACC-1", Currency.USD);
      account.deposit(new BigDecimal("100.00"));
      assertEquals(new BigDecimal("100.00"), account.balance());
  }

  @Test
  void withdrawDecreasesBalance() {
      Account account = new Account("ACC-1", Currency.USD);
      account.deposit(new BigDecimal("100.00"));
      account.withdraw(new BigDecimal("30.00"));
      assertEquals(new BigDecimal("70.00"), account.balance());
  }

  // after - common setup in a fixture, test shows only what is unique
  private Account account;

  @BeforeEach
  void setUp() {
      account = new Account("ACC-1", Currency.USD);
      account.deposit(new BigDecimal("100.00"));
  }

  @Test
  void withdrawDecreasesBalance() {
      account.withdraw(new BigDecimal("30.00"));
      assertEquals(new BigDecimal("70.00"), account.balance());
  }
  ```

## Other

- Tests that depend on manual environment/config setup ("works on my machine").
- Hidden assertions or test data, buried behind layers of framework abstraction.
