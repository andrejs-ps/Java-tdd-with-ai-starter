package com.tddai.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust counterpart of {@link DemoMain#calculateNum}: returns the num (in percent, 2 decimals, HALF_UP)
 * of every valid line. Invalid lines are skipped and written to a timestamped errors file;
 * an unusable file (missing, empty, too big) throws {@link InvalidFileStateException}.
 */
public class RobustNumCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path errorsDir;
    private final long maxFileSizeBytes;

    public RobustNumCalculator(Path errorsDir, long maxFileSizeBytes) {
        this.errorsDir = errorsDir;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<BigDecimal> calculateNum(String fileName) throws InvalidFileStateException {
        Path path = locate(fileName);
        List<BigDecimal> validLines = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(path)) { // strict UTF-8, one line at a time
            long size = Files.size(path);
            if (size == 0) {
                throw new InvalidFileStateException(fileName + " is empty");
            }
            if (size > maxFileSizeBytes) {
                throw new InvalidFileStateException(fileName + " is too big: more than " + maxFileSizeBytes + " bytes");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    validLines.add(numOf(line));
                } catch (IllegalArgumentException e) {
                    errors.add(fileName + ":" + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new InvalidFileStateException("cannot read " + fileName + " (invalid UTF-8 or I/O error)", e);
        }

        writeErrors(errors);
        return validLines;
    }

    private void writeErrors(List<String> errors) throws InvalidFileStateException {
        if (errors.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(errorsDir);
            Files.write(errorsDir.resolve("errors-" + TIMESTAMP.format(LocalDateTime.now()) + ".txt"), errors);
        } catch (IOException e) {
            throw new InvalidFileStateException("cannot write errors file to " + errorsDir, e);
        }
    }

    private Path locate(String fileName) throws InvalidFileStateException {
        URL url = RobustNumCalculator.class.getResource("/demo/" + fileName);
        if (url == null) {
            throw new InvalidFileStateException("file not found: /demo/" + fileName);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new InvalidFileStateException("cannot locate " + fileName, e);
        }
    }

    private static BigDecimal numOf(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 2) {
            throw invalid(line);
        }
        BigDecimal dividend;
        BigDecimal price;
        try {
            dividend = new BigDecimal(columns[0]); // rejects empty, NaN, Infinity, 1d, 0x1p3, which Double.parseDouble accepts
            price = new BigDecimal(columns[1]);
        } catch (NumberFormatException e) {
            throw invalid(line);
        }
        if (dividend.signum() < 0 || price.signum() <= 0) {
            throw invalid(line);
        }
        return dividend.multiply(HUNDRED).divide(price, 2, RoundingMode.HALF_UP);
    }

    private static IllegalArgumentException invalid(String line) {
        return new IllegalArgumentException("Line has invalid values: " + line);
    }
}
