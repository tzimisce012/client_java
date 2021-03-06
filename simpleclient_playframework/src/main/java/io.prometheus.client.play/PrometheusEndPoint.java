package io.prometheus.client.play;


import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enable an endpoint that exposes Prometheus metrics from its default collector.
 * <p>
 * Usage:
 * <br>You need to make visible this controller to the endpoint /metrics in your routes file, e.g.:
 * <pre>
 * GET /metrics        com.telefonica.prometheus.PrometheusController.getMetrics()
 * </pre>
 * <p>
 * This controller will start some collectors for garbage collection, memory pools, JMX, classloading, and thread counts.
 *
 * @author Daniel Ochoa
 */
public class PrometheusEndPoint extends Controller {

    private final CollectorRegistry collectorRegistry;

    public PrometheusEndPoint() {
        this.collectorRegistry = CollectorRegistry.defaultRegistry;
    }

    public Result getMetrics() {
        String[] name = request().queryString().get("name[]");
        String result;

        if (name == null) {
            result = writeRegistry(Collections.emptySet());
        } else {
            Set<String> setName = new HashSet<>(Arrays.asList(name));
            result = writeRegistry(setName);
        }

        return Results.ok(result);
    }

    private String writeRegistry(Set<String> metricsToInclude) {
        try {
            Writer writer = new StringWriter();
            TextFormat.write004(writer, collectorRegistry.filteredMetricFamilySamples(metricsToInclude));
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Writing metrics failed", e);
        }
    }
}
