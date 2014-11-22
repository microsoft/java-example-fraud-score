/*
 * FraudController.java
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
 * Spring REST Controller
 *
 * Mapping: /pool/init/{size} supports (re)building RBroker runtime.
 *
 * Mapping: /fraud/score/{tasks} supports RTask execution on RBroker runtime.
 */
package com.revo.deployr.rbroker.example.controller;

import com.revo.deployr.rbroker.example.service.FraudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.apache.log4j.Logger;

@RestController
@RequestMapping("/fraud")
public class FraudController {

    private static Logger log = Logger.getLogger(FraudController.class);
    private final FraudService fraudService;

    @Autowired
    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @RequestMapping(value = "/pool/init/{size}", method = RequestMethod.POST)
    public void buildPool(@PathVariable("size") int size) {

        try {

            size = (size == 0) ? 1 : size;
            log.info("REST:/pool/init/" + size + " called.");

            /*
             * Create a new pool or resize an existing pool.
             */
            fraudService.buildPool(size);

        } catch(Exception ex) {
            log.warn("FraudController: buildPool ex=" + ex);
            String cause = ex.getMessage();
            String msg = "RBroker runtime pool initialization failed: " + cause;
            fraudService.alertClient(msg, cause, true);
        }
    }

    /*
     * Score one or more demo account(s) with randomly generated data
     * for fraud.
     *
     * A call to /fraud/score/N will result in N RTasks, representing
     * N demo tasks with randomly generated data, being scored.
     */
    @RequestMapping(value = "/score/{tasks}", method = RequestMethod.GET)
    public void score(@PathVariable("tasks") int tasks) {

        tasks = (tasks == 0) ? 1 : tasks;
        log.info("REST:/score/" + tasks + " called.");

        for(int i=0; i<tasks;i++) {
            try {
                /*
                 * FraudService.buildTask creates an RTask using
                 * randomly generated demo account data.
                 *
                 * FraudService.submit takes that RTask and passes it
                 * to an instance of RBroker for execution.
                 */
                fraudService.submit(fraudService.buildTask());

            } catch(Exception ex) {
                log.warn("FraudController: score ex=" + ex);
            }
        }
    }

}
