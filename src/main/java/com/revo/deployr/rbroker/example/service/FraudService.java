/*
 * FraudService.java
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
 * Spring Boot service exposing RBroker runtime services
 * to client applications via FraudController REST API.
 */
package com.revo.deployr.rbroker.example.service;

import com.revo.deployr.rbroker.example.model.FraudScore;
import com.revo.deployr.rbroker.example.model.RuntimeStats;
import com.revo.deployr.rbroker.example.model.ClientAlert;

import com.revo.deployr.client.broker.*;
import com.revo.deployr.client.broker.config.*;
import com.revo.deployr.client.broker.task.*;
import com.revo.deployr.client.broker.options.*;
import com.revo.deployr.client.params.*;
import com.revo.deployr.client.data.*;
import com.revo.deployr.client.auth.*;
import com.revo.deployr.client.auth.basic.*;
import com.revo.deployr.client.factory.RBrokerFactory;
import com.revo.deployr.client.factory.RTaskFactory;
import com.revo.deployr.client.factory.RDataFactory;

import com.revo.deployr.rbroker.example.util.RBrokerStatsHelper;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;
import org.apache.log4j.Logger;

@Service
public class FraudService
            implements RTaskListener, RBrokerListener {

    private static Logger log = Logger.getLogger(FraudService.class);

    private final SimpMessagingTemplate simpMessagingTemplate;
    private RBroker rBroker = null;
    private PooledBrokerConfig brokerConfig = null;
    private int lastAllocatedPoolSize = 0;
    private static final String FRAUDMSGTOPIC = "/topic/fraud";

    @Autowired
    public FraudService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /*
     * Service Method:
     *
     * buildPool(int poolSize)
     *
     * Supports: FraudController POST:/pool/init/{size}.
     */
    public void buildPool(int poolSize) {

        try {

            if(rBroker == null) {

                /*
                 * Create initial RBroker instance.
                 */

                RAuthentication rAuth =
                    new RBasicAuthentication(System.getProperty("username"),
                                             System.getProperty("password"));

                PoolCreationOptions poolOptions = new PoolCreationOptions();

                PoolPreloadOptions preloadOptions = 
                                        new PoolPreloadOptions();
                preloadOptions.filename = "fraudModel.rData";
                preloadOptions.directory = "example-fraud-score";
                preloadOptions.author = "testuser";
                poolOptions.preloadWorkspace = preloadOptions;
                String endpoint = System.getProperty("endpoint");

                /*
                 * Ensure releaseGridResources property is enabled
                 * so server-side grid resource management will auto
                 * clear prior pool resources before creating a new
                 * pool on a refresh.
                 */
                poolOptions.releaseGridResources = true;

                brokerConfig = new PooledBrokerConfig(endpoint,
                                                      rAuth,
                                                      poolSize,
                                                      poolOptions);

                rBroker = RBrokerFactory.pooledTaskBroker(brokerConfig);
                lastAllocatedPoolSize = rBroker.maxConcurrency();

                rBroker.addTaskListener(this);
                rBroker.addBrokerListener(this);

                log.info("RBroker pool initialized with " +
                    lastAllocatedPoolSize + " R sessions.");

            } else {

                /*
                 * Release old instance of RBroker.
                 */
                rBroker.shutdown();

                /*
                 * Create new instance of RBroker.
                 */
                brokerConfig.maxConcurrentTaskLimit = poolSize;
                rBroker = RBrokerFactory.pooledTaskBroker(brokerConfig);
                lastAllocatedPoolSize = rBroker.maxConcurrency();

                log.info("RBroker pool resized to " +
                    lastAllocatedPoolSize + " R sessions.");

                rBroker.addTaskListener(this);
                rBroker.addBrokerListener(this);

            }

            RuntimeStats runtimeStats = new RuntimeStats();
            runtimeStats = populateRuntimeStats(runtimeStats, null);
            /*
             * Push RuntimeStats message over STOMP Web Socket to clients
             * listening on FRAUDMSGTOPIC.
             */
            simpMessagingTemplate.convertAndSend(FRAUDMSGTOPIC, runtimeStats);

        } catch(Exception ex) {
            log.warn("FraudService: init ex=" + ex);
        }
    }

    /*
     * Service Methods:
     *
     * buildTask()
     * submitTask(RTask rTask)
     *
     * Supports: FraudController GET:/fraud/score/{tasks}.
     */
    public RTask buildTask() {

        RTask rTask = null;

        try {

            PooledTaskOptions taskOptions = new PooledTaskOptions();
            taskOptions.routputs = Arrays.asList("x");

            int bal = Math.abs((new Random()).nextInt() % 25000);
            int trans = Math.abs((new Random()).nextInt() % 100);
            int credit = Math.abs((new Random()).nextInt() % 75);

            taskOptions.rinputs = Arrays.asList(
                (RData) RDataFactory.createNumeric("bal", bal),
                (RData) RDataFactory.createNumeric("trans", trans),
                (RData) RDataFactory.createNumeric("credit", credit)
            );

            rTask = RTaskFactory.pooledTask("ccFraudScore",
                                            "example-fraud-score",
                                            "testuser",
                                            null, taskOptions);

        } catch(Exception ex) {
            log.warn("FraudController: buildTask, " +
                                                    "ex=" + ex);
        }

        return rTask;
    }


    public void submit(RTask rTask) {

        if(rBroker != null && rTask != null) {

            try {
                RTaskToken taskToken = rBroker.submit(rTask);
            } catch(Exception ex) {
                log.warn("FraudService: submitted RTask ex=" + ex);
            }
        }
    }

    /*
     * RBroker RTaskListener Implementation.
     */

    public void onTaskCompleted(RTask rTask, RTaskResult rTaskResult) {

        RBrokerStatsHelper.printRTaskResult(rTask, rTaskResult, null);

        FraudScore fraudScore = buildFraudScore(rTask, rTaskResult);
        // Push FraudScore message over STOMP Web Socket to clients.
        simpMessagingTemplate.convertAndSend(FRAUDMSGTOPIC, fraudScore);
    }

    public void onTaskError(RTask rTask, Throwable throwable) {
        RBrokerStatsHelper.printRTaskResult(rTask, null, throwable);
        // Push FraudScore message over STOMP Web Socket to clients.
        FraudScore fraudScore = buildFraudScore(rTask, null);
        simpMessagingTemplate.convertAndSend(FRAUDMSGTOPIC, fraudScore);

        if(rBroker != null & !rBroker.isConnected()) {
            /*
             * If RTask is failing because the RBroker runtime has
             * lost it's connection to the DeployR server then broadcast
             * an alert message indicating problem and potential remedy.
             */
            String msg = "The RBroker runtime has lost it's connection " +
                "to DeployR server. Try building a new pool using Resize.";
            String cause = throwable.getMessage();
            alertClient(msg, cause);
        }
    }

    /*
     * RBroker RBrokerListener Implementation.
     */

    public void onRuntimeError(Throwable throwable) {
        /*
         * In case of an unexpected runtime error from the RBroker
         * runtime broadcast an alert message.
         */
        String cause = throwable.getMessage();
        String msg = "The RBroker runtime has indicated an unexpected " +
           " runtime error has occured. Cause: " + cause;
        alertClient(msg, cause);
    }

    public void onRuntimeStats(RBrokerRuntimeStats stats, int maxConcurrency) {
        RBrokerStatsHelper.printRBrokerStats(stats, maxConcurrency);

        RuntimeStats runtimeStats = new RuntimeStats();
        runtimeStats = populateRuntimeStats(runtimeStats, stats);
        /*
         * Push RuntimeStats message over STOMP Web Socket to clients
         * listening on FRAUDMSGTOPIC.
         */
        simpMessagingTemplate.convertAndSend(FRAUDMSGTOPIC, runtimeStats);
    }

    /*
     * Builds a FraudScore object that encapsulates an RTask result
     * that is then pushed as a STOMP message to clients that have
     * subscribed on FRAUDMSGTOPIC. 
     */
    private FraudScore buildFraudScore(RTask rTask, RTaskResult rTaskResult) {

        FraudScore fraudScore = new FraudScore();

        if(rTaskResult != null)
            fraudScore.success = true;

        try {

            List<RData> rinputs =
                ((PooledTaskOptions)((PooledTask)rTask).options).rinputs;

            fraudScore.balance = (int) ((RNumeric)rinputs.get(0)).getValue();
            fraudScore.transactions = (int) ((RNumeric)rinputs.get(1)).getValue();
            fraudScore.credit = (int) ((RNumeric)rinputs.get(2)).getValue();

            if(rTaskResult != null) {
                List<RData> rObjects = rTaskResult.getGeneratedObjects();
                fraudScore.score = ((RNumeric)rObjects.get(0)).getValue();
            }

        } catch(Exception ex) {
            log.warn("buildFraudScore: ex=" + ex);
        }

        return fraudScore;
    }

    /*
     * Private helper methods.
     */

    private RuntimeStats populateRuntimeStats(RuntimeStats runtimeStats,
                                           RBrokerRuntimeStats stats) {

        runtimeStats.requestedPoolSize = brokerConfig.maxConcurrentTaskLimit;
        runtimeStats.allocatedPoolSize = lastAllocatedPoolSize;

        runtimeStats.endpoint = brokerConfig.deployrEndpoint;
        if(brokerConfig.userCredentials != null) {
            runtimeStats.username =
                ((RBasicAuthentication) brokerConfig.userCredentials).getUsername();
        }

        if(stats != null) {

            runtimeStats.submittedTasks = stats.totalTasksRun;
            runtimeStats.successfulTasks = stats.totalTasksRunToSuccess;
            runtimeStats.failedTasks = stats.totalTasksRunToFailure;

            runtimeStats.averageCodeExecution = 0L;
            runtimeStats.averageServerOverhead = 0L;
            runtimeStats.averageNetworkLatency = 0L;

            if(stats.totalTasksRunToSuccess > 0) {
                runtimeStats.averageCodeExecution =
                    stats.totalTimeTasksOnCode/stats.totalTasksRunToSuccess;
                long avgTimeOnServer =
                    stats.totalTimeTasksOnServer/stats.totalTasksRunToSuccess;
                runtimeStats.averageServerOverhead =
                    avgTimeOnServer - runtimeStats.averageCodeExecution;
                long avgTimeOnCall =
                    stats.totalTimeTasksOnCall/stats.totalTasksRunToSuccess;
                runtimeStats.averageNetworkLatency =
                    avgTimeOnCall - avgTimeOnServer;
            }
        }

        return runtimeStats;
    }

    public void alertClient(String msg, String cause) {

        try {

            ClientAlert clientAlert = new ClientAlert();
            clientAlert.msg = msg;
            clientAlert.cause = cause;
            // Push ClientAlert message over STOMP Web Socket to clients.
            simpMessagingTemplate.convertAndSend(FRAUDMSGTOPIC, clientAlert);

        } catch(Exception cex) {}

    }

    /*
     * Spring Lifecycle Event Handler
     *
     * Service: destroy
     */
    @PreDestroy
    public void destroy() throws Exception {
        if(rBroker != null) {
            rBroker.shutdown();
        }
    }
}
