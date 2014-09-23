/*
 * FraudEngine.java
 *
 * Copyright (C) 2010-2014 by Revolution Analytics Inc.
 *
 * This program is licensed to you under the terms of Version 2.0 of the
 * Apache License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0) for more details.
 *
 */
/*
 * Java Fraud Detection Example Application
 *
 * Spring Boot application (context) initialization.
 */
package com.revo.deployr.rbroker.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import org.apache.log4j.Logger;

@ComponentScan
@EnableAutoConfiguration
public class FraudEngine {

    private static Logger log = Logger.getLogger(FraudEngine.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FraudEngine.class);
        // Ensures @PreDestroy called on kill -1 <<pid>>.
        app.setRegisterShutdownHook(true);
        app.setShowBanner(false);
        app.run(args);
        log.info("Fraud Score application server has started.");
        log.info("Start the Fraud Score client application in your Web browser:\n");
        log.info("http://localhost:9080/\n");
    }
}
