/*
 * RuntimeStats.java
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
 * RUNTIMESTATS - RBroker runtime statistics message.
 *
 * Delivered over STOMP to subscribers on /topic/fraud.
 */
package com.revo.deployr.rbroker.example.model;

import lombok.Data;

public @Data class RuntimeStats {

    /*
     * RBroker
     *
     * Endpoint and authenticated user.
     */
    public String endpoint;
    public String username;

    /*
     * RBroker
     *
     * PooledTaskBroker pool statistics.
     */
    public int requestedPoolSize;
    public int allocatedPoolSize;
    public int maxConcurrency;

    /*
     * RTask Throughput
     */
    public long submittedTasks;
    public long successfulTasks;
    public long failedTasks;

    /*
     * RTask Timing (Averages)
     */
    public long averageCodeExecution;
    public long averageServerOverhead;
    public long averageNetworkLatency;

    public final String msgType = "RUNTIMESTATS";
}
