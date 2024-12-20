/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.async;

import com.newrelic.agent.TokenImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AsyncTransactionServiceTest {

    @Test(timeout = 90000)
    public void testAsyncTransactionServiceNoTimeout() throws Exception {
        System.out.println("JGB AsyncTxServiceA: "+ServiceFactory.getAsyncTxService());
        TransactionAsyncUtility.createServiceManager(createConfigMap(90000));
        System.out.println("JGB AsyncTxServiceB: "+ServiceFactory.getAsyncTxService());

        Transaction.clearTransaction();
        TokenImpl token = new TokenImpl(null);
        assertTrue(ServiceFactory.getAsyncTxService().putIfAbsent("myFirstKey", token));
        assertTrue(ServiceFactory.getAsyncTxService().putIfAbsent("mySecondKey", token));
        assertFalse(ServiceFactory.getAsyncTxService().putIfAbsent("myFirstKey", token));
        assertFalse(ServiceFactory.getAsyncTxService().putIfAbsent("mySecondKey", token));

        assertEquals(token, ServiceFactory.getAsyncTxService().extractIfPresent("myFirstKey"));
        assertNull(ServiceFactory.getAsyncTxService().extractIfPresent("myFirstKey"));

        assertEquals(token, ServiceFactory.getAsyncTxService().extractIfPresent("mySecondKey"));
        assertNull(ServiceFactory.getAsyncTxService().extractIfPresent("mySecondKey"));
    }

    @Test(timeout = 90000)
    public void testAsyncTransactionServiceForceTimeout() throws Exception {
        System.out.println("JGB AsyncTxService1: "+ServiceFactory.getAsyncTxService());
        TransactionAsyncUtility.createServiceManager(createConfigMap(1));
        System.out.println("JGB AsyncTxService2: "+ServiceFactory.getAsyncTxService());
        System.out.println("JGB timeout in test secs: "+ServiceFactory.getConfigService().getDefaultAgentConfig().getTokenTimeoutInSec());

        assertEquals(0, ServiceFactory.getAsyncTxService().cacheSizeForTesting());
        ServiceFactory.getAsyncTxService().beforeHarvest("test", null);

        Transaction.clearTransaction();
        TokenImpl token = new TokenImpl(null);
        assertTrue(ServiceFactory.getAsyncTxService().putIfAbsent("myFirstKey", token));
        assertTrue(ServiceFactory.getAsyncTxService().putIfAbsent("mySecondKey", token));
        assertFalse(ServiceFactory.getAsyncTxService().putIfAbsent("myFirstKey", token));
        assertFalse(ServiceFactory.getAsyncTxService().putIfAbsent("mySecondKey", token));
        assertEquals(2, ServiceFactory.getAsyncTxService().cacheSizeForTesting());

        Thread.sleep(5000);
        ServiceFactory.getAsyncTxService().cleanUpPendingTransactions();

        for (int i=0;i<25;i++) {
            System.out.println("JGB cache size: "+ServiceFactory.getAsyncTxService().cacheSizeForTesting());
            if (0 == ServiceFactory.getAsyncTxService().cacheSizeForTesting()) break;
            try { Thread.sleep(1000); } catch (Exception e) {}
            ServiceFactory.getAsyncTxService().cleanUpPendingTransactions();
        }

        assertEquals("Cache should be empty", 0, ServiceFactory.getAsyncTxService().cacheSizeForTesting());
        assertNull(ServiceFactory.getAsyncTxService().extractIfPresent("myFirstKey"));
        assertNull(ServiceFactory.getAsyncTxService().extractIfPresent("mySecondKey"));

        ServiceFactory.getAsyncTxService().beforeHarvest("test", null);
        assertEquals(0, ServiceFactory.getAsyncTxService().cacheSizeForTesting());
    }

    private static Map<String, Object> createConfigMap(int timeoutInSeconds) {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put("token_timeout", timeoutInSeconds);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

}
