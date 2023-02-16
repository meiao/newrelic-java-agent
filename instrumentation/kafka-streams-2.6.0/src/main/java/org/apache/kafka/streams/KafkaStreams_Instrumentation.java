package org.apache.kafka.streams;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.NewRelicMetricsReporter;
import org.apache.kafka.common.metrics.Metrics;

@Weave(originalName = "org.apache.kafka.streams.KafkaStreams")
public class KafkaStreams_Instrumentation {
    private final Metrics metrics = Weaver.callOriginal();

    @NewField
    private boolean initialized;

    @WeaveAllConstructors
    public KafkaStreams_Instrumentation() {
        if (!initialized) {
            metrics.addReporter(new NewRelicMetricsReporter());
            initialized = true;
        }
    }
}
