package com.tddai.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Happy-path only, on purpose. Change the file name to see how it breaks.
 * See src/main/resources/demo/README.md for what each file triggers.
 */
public class DemoMain {

    static void main() throws Exception {
        var list = calculateNum("data.csv");
        System.out.println(list);
        System.out.println(list.getFirst());
    }

    /**
     * 1) Fetch a CSV file
     * 2) Loop over each line [a,b]
     * 3) Calculate ( a / b) * 100
     *
     * Example:
     * 10,2
     * 100,10
     *
     * list[5000, 1000]
     *
     * */
    public static List<Double> calculateNum(String fileName) throws Exception {
        Path path = Path.of(DemoMain.class.getResource("/demo/" + fileName).toURI());

        List<Double> computedNums = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String[] columns = line.split(",");
            double a = Double.parseDouble(columns[0]);
            double b = Double.parseDouble(columns[1]);
            computedNums.add(a / b * 100);
        }
        return computedNums;
    }
}
