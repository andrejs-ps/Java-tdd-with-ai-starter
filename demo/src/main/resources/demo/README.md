## Demo files: what each CSV triggers in `DemoMain.calculateNum` (happy path)

### Works

- `data.csv`
  - Good data, no problem → `[5.0, 3.0]`

### Throws an exception

- *(a file name that doesn't exist)*
  - The file is not there → `NullPointerException`
- `bad-number.csv`
  - A value that isn't a valid number (e.g. `abc,50`) → `NumberFormatException`
  - A missing value in the first column (e.g. `,50`) → `NumberFormatException`
  - A blank line → `NumberFormatException`
- `missing-column.csv`
  - A missing value in the second column (e.g. `5` or `5,`) → `ArrayIndexOutOfBoundsException`
- `big-file.csv`
  - The file is too big for the heap (run with VM option `-Xmx32m`) → `OutOfMemoryError`
- `latin1-encoding.csv`
  - The file is not valid UTF-8 (e.g. a Latin-1 "é", byte `0xE9`) → `MalformedInputException`

### Silently gives a wrong result (no exception)

- `empty.csv`
  - The file is empty → returns an empty list, exception if client code doesn't handle it (e.g. `list.getFirst()`)
- `silent-wrong-values.csv`
  - Division by zero (e.g. `2.50,0`) → no exception, `Infinity` (`0,0` gives `NaN`)
  - A negative value (e.g. `-2.50,50`) → no exception, a negative num
  - A value Java parses as a number but isn't one (e.g. `NaN,50`) → no exception, `NaN` or `Infinity`
  - A value too big for a `double` (e.g. `1e999,50`) → no exception, `Infinity`
  - A result too big for a `double` (e.g. `1e307,0.001`) → no exception, `Infinity`
- `silent-wrong-columns.csv`
  - Extra columns (e.g. `2.50,50,999`) → no exception, extra columns ignored
- `rounding.csv`
  - Binary `double` rounding error (e.g. `2,3`) → no exception, `66.66666666666666` instead of `66.67`

### Additional possible bugs

- With `double` and `float`, division by zero may result in either `Infinity` or `NaN` - no exception happens and the likely buggy value travels through the system. But with `int`, `long` or `BigDecimal` you get `ArithmeticException`  

The robust impl (`RobustNumCalculator`) reads the file line by line and returns the `BigDecimal` nums (2 decimals, HALF_UP)
of the valid lines. Every invalid line is written to `errors-<yyyyMMdd-HHmmss>.txt`.
It throws `InvalidFileStateException` when the whole file is unusable (missing, empty, too big, not UTF-8).
