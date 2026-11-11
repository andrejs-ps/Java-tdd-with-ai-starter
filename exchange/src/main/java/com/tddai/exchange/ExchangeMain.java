package com.tddai.exchange;

import com.tddai.exchange.config.ExchangeConfig;

public class ExchangeMain {

    static void main() {
        var config = ExchangeConfig.load();
        ExchangeApp.start(config);
    }
}
