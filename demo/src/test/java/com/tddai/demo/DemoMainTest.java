package com.tddai.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemoMainTest {

    @Test
    void goodDataGivesNums() throws Exception {
        List<Double> nums = DemoMain.calculateNum("data.csv");
        assertEquals(5.0, nums.get(0));
        assertEquals(3.0, nums.get(1));
    }
}
