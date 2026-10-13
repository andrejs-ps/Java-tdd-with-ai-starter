package com.tddai.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobustNumCalculatorTest {

    @TempDir
    Path errorsDir;

    private RobustNumCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RobustNumCalculator(errorsDir, 500L * 1024 * 1024);
    }

    @Test
    void goodDataProducesNoErrorsFile() throws Exception {
        assertEquals(decimals("5.00", "3.00"), numsOf("data.csv"));
        assertEquals(List.of(), errorFiles());
    }

    // --- the whole file is unusable ---

    @Test
    void missingFileIsRejected() {
        InvalidFileStateException e = assertThrows(InvalidFileStateException.class, () -> numsOf("no-such-file.csv"));
        assertTrue(e.getMessage().contains("no-such-file.csv"));
    }

    @Test
    void emptyFileIsRejected() {
        InvalidFileStateException e = assertThrows(InvalidFileStateException.class, () -> numsOf("empty.csv"));
        assertTrue(e.getMessage().contains("empty"));
    }

    @Test
    void nonUtf8FileIsRejected() {
        InvalidFileStateException e = assertThrows(InvalidFileStateException.class, () -> numsOf("latin1-encoding.csv"));
        assertTrue(e.getMessage().contains("UTF-8"));
    }

    @Test
    void tooBigFileIsRejected() {
        // big-file.csv is 16 MB
        RobustNumCalculator smallLimit = new RobustNumCalculator(errorsDir, 10L * 1024 * 1024);
        InvalidFileStateException e = assertThrows(InvalidFileStateException.class, () -> smallLimit.calculateNum("big-file.csv"));
        assertTrue(e.getMessage().contains("too big"));
    }

    // --- bad lines are skipped and reported, the good ones still count ---

    @Test
    void unparseableOrEmptyNumbersAreReported() throws Exception {
        assertEquals(decimals("3.00"), numsOf("bad-number.csv"));
        assertEquals(List.of(
                "bad-number.csv:1: Line has invalid values: abc,50",
                "bad-number.csv:2: Line has invalid values: ,50",
                "bad-number.csv:3: Line has invalid values: 2.50,xyz",
                "bad-number.csv:4: Line has invalid values: "), errorLines());
    }

    @Test
    void missingColumnsAreReported() throws Exception {
        assertEquals(decimals("3.00"), numsOf("missing-column.csv"));
        assertEquals(List.of(
                "missing-column.csv:1: Line has invalid values: 5",
                "missing-column.csv:2: Line has invalid values: 5,"), errorLines());
    }

    @Test
    void extraColumnsAreReportedInsteadOfIgnored() throws Exception {
        assertEquals(decimals("3.00"), numsOf("silent-wrong-columns.csv"));
        assertEquals(List.of(
                "silent-wrong-columns.csv:1: Line has invalid values: 2.50,50,999",
                "silent-wrong-columns.csv:2: Line has invalid values: 3,60,70,80",
                "silent-wrong-columns.csv:3: Line has invalid values: 2.50,50,"), errorLines());
    }

    @Test
    void nonsensicalValuesAreReportedInsteadOfSilentlyCalculated() throws Exception {
        // BigDecimal has no overflow: the two huge values are calculated correctly, where double gave Infinity
        assertEquals(decimals("2E+999", "1E+312", "5.00"), numsOf("silent-wrong-values.csv"));
        assertEquals(List.of(
                "silent-wrong-values.csv:1: Line has invalid values: 2.50,0",
                "silent-wrong-values.csv:2: Line has invalid values: 0,0",
                "silent-wrong-values.csv:3: Line has invalid values: -2.50,50",
                "silent-wrong-values.csv:4: Line has invalid values: 2.50,-50",
                "silent-wrong-values.csv:5: Line has invalid values: NaN,50",
                "silent-wrong-values.csv:6: Line has invalid values: Infinity,50"), errorLines());
    }

    @Test
    void errorsFileNameCarriesTheTimestamp() throws Exception {
        numsOf("bad-number.csv");
        assertTrue(errorFiles().getFirst().matches("errors-\\d{8}-\\d{6}\\.txt"));
    }

    // --- rounding ---

    @Test
    void numsAreRoundedHalfUpToTwoDecimalsWithoutBinaryDrift() throws Exception {
        // as doubles these come out as 10.000000000000002, 66.66666666666666 and 33.333333333333336
        assertEquals(decimals("10.00", "66.67", "33.33"), numsOf("rounding.csv"));
    }

    private List<BigDecimal> numsOf(String fileName) throws InvalidFileStateException {
        return calculator.calculateNum(fileName);
    }

    private static List<BigDecimal> decimals(String... values) {
        return Stream.of(values).map(value -> new BigDecimal(value).setScale(2)).toList();
    }

    private List<String> errorFiles() throws IOException {
        try (Stream<Path> files = Files.list(errorsDir)) {
            return files.map(file -> file.getFileName().toString()).toList();
        }
    }

    private List<String> errorLines() throws IOException {
        List<String> names = errorFiles();
        assertEquals(1, names.size());
        return Files.readAllLines(errorsDir.resolve(names.get(0)));
    }
}
