package com.tddai.broker;

import com.tddai.broker.config.BrokerConfig;

public class BrokerMain {

    static void main() {
        var config = BrokerConfig.load();
        BrokerApp.start(config);
    }
}
