/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.contrib.sample.stream;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.metric.sample.HystrixUtilization;
import com.netflix.hystrix.metric.sample.HystrixUtilizationStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Streams Hystrix config in text/event-stream format.
 * <p>
 * Install by:
 * <p>
 * 1) Including hystrix-metrics-event-stream-*.jar in your classpath.
 * <p>
 * 2) Adding the following to web.xml:
 * <pre>{@code
 * <servlet>
 *  <description></description>
 *  <display-name>HystrixUtilizationSseServlet</display-name>
 *  <servlet-name>HystrixUtilizationSseServlet</servlet-name>
 *  <servlet-class>com.netflix.hystrix.contrib.sample.stream.HystrixUtilizationSseServlet</servlet-class>
 * </servlet>
 * <servlet-mapping>
 *  <servlet-name>HystrixUtilizationSseServlet</servlet-name>
 *  <url-pattern>/hystrix/utilization.stream</url-pattern>
 * </servlet-mapping>
 * } </pre>
 */
public class HystrixUtilizationSseServlet extends HystrixSampleSseServlet {

    private static final long serialVersionUID = -7812908330777694972L;
    private static final Logger logger = LoggerFactory.getLogger(HystrixUtilizationSseServlet.class);

    /* used to track number of connections and throttle */
    private static AtomicInteger concurrentConnections = new AtomicInteger(0);
    private static DynamicIntProperty maxConcurrentConnections =
            DynamicPropertyFactory.getInstance().getIntProperty("hystrix.config.stream.maxConcurrentConnections", 5);

    public HystrixUtilizationSseServlet() {
        this(HystrixUtilizationStream.getInstance().observe(), DEFAULT_PAUSE_POLLER_THREAD_DELAY_IN_MS);
    }

    /* package-private */ HystrixUtilizationSseServlet(Observable<HystrixUtilization> sampleStream, int pausePollerThreadDelayInMs) {
        super(sampleStream.map(new Func1<HystrixUtilization, String>() {
            @Override
            public String call(HystrixUtilization hystrixUtilization) {
                try {
                    return HystrixUtilizationJsonStream.convertToJson(hystrixUtilization);
                } catch (IOException ioe) {
                    logger.error("IOException creating JSON from HystrixUtilization", ioe);
                    return "<IOException> : " + ioe.getMessage();
                }
            }
        }), pausePollerThreadDelayInMs);
    }

    @Override
    protected int getMaxNumberConcurrentConnectionsAllowed() {
        return maxConcurrentConnections.get();
    }

    @Override
    protected int getNumberCurrentConnections() {
        return concurrentConnections.get();
    }

    @Override
    protected int incrementAndGetCurrentConcurrentConnections() {
        return concurrentConnections.incrementAndGet();
    }

    @Override
    protected void decrementCurrentConcurrentConnections() {
        concurrentConnections.decrementAndGet();
    }
}

