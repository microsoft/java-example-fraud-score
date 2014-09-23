/*
 * RBrokerStatsHelper.java
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
 * RBroker runtime summary statistics (print) helper class.
 */
package com.revo.deployr.rbroker.example.util;

import com.revo.deployr.client.broker.RTask;
import com.revo.deployr.client.broker.RTaskType;
import com.revo.deployr.client.broker.RTaskResult;
import com.revo.deployr.client.broker.RBrokerRuntimeStats;

import org.apache.log4j.Logger;

public class RBrokerStatsHelper {

    private static Logger log = Logger.getLogger(RBrokerStatsHelper.class);

    /**
     * Prints {@link com.revo.deployr.client.broker.RBrokerRuntimeStats}
     * to console output.
     */
    public static void printRBrokerStats(RBrokerRuntimeStats stats,
                                         int maxConcurrency) {

        log.info("RBroker Activity Summary");
        log.info("RBroker: Max Concurrency [ " +
                            maxConcurrency + " ]");
        log.info("RBroker: Total Tasks Run [ " +
                            stats.totalTasksRun + " ]");

        log.info("RBroker: Tasks Ok [ " +
                stats.totalTasksRunToSuccess + " ] Fail [ " +
                stats.totalTasksRunToFailure + " ]");

        long displayAvgTimeOnCode = 0L;
        long displayAvgTimeOnServer = 0L;
        long displayAvgTimeOnCall = 0L;

        if(stats.totalTasksRunToSuccess > 0) {
            displayAvgTimeOnCode =
                stats.totalTimeTasksOnCode/stats.totalTasksRunToSuccess;
            displayAvgTimeOnServer =
                stats.totalTimeTasksOnServer/stats.totalTasksRunToSuccess;
            displayAvgTimeOnCall =
                stats.totalTimeTasksOnCall/stats.totalTasksRunToSuccess;
        }

        log.info("RBroker: Task Average Time On Code [ " +
                                                displayAvgTimeOnCode + " ]");
        log.info("RBroker: Task Average Time On Server [ " +
                                                displayAvgTimeOnServer + " ]");
        log.info("RBroker: Task Average Time On Call   [ " +
                                                displayAvgTimeOnCall + " ]\n");
    }

    /**
     * Prints {@link com.revo.deployr.client.broker.RTaskResult}
     * to console output.
     */
    public static void printRTaskResult(RTask task,
                                        RTaskResult result,
                                        Throwable throwable) {

        log.info("Task: " + task);

        if(throwable != null) {

           log.warn("Status[fail]: cause=" + throwable);

        } else {

            switch(result.getType()) {

                case DISCRETE:
                    if(result.isSuccess()) {
                        log.info("Status[ok]: [ code : " +
                                result.getTimeOnCode() + " , server : " +
                                result.getTimeOnServer() + " , call : " +
                                result.getTimeOnCall() + " ]");
                    } else {
                        log.warn("Status[fail]: cause=" +
                                                        result.getFailure());
                    }
                break;

                case POOLED:
                    if(result.isSuccess()) {
                        log.info("Status[ok]: [ code : " +
                                result.getTimeOnCode() + " , server : " +
                                result.getTimeOnServer() + " , call : " +
                                result.getTimeOnCall() + " ]");
                    } else {
                        log.warn("Status[fail]: cause=" +
                                                        result.getFailure());
                    }
                break;

                case BACKGROUND:
                    if(result.isSuccess()) {
                        log.info("Status[ok]: [ server : " +
                                result.getTimeOnServer() + " , call : " +
                                result.getTimeOnCall() + " ]");
                    } else {
                        log.warn("Status[fail]: cause=" +
                                                        result.getFailure());
                    }
                break;

            }
        }
    }

}
