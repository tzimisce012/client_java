package io.prometheus.client.play;

import akka.stream.Materializer;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * This filter enables some default metrics for your play application
 * <p>
 * Usage:
 * <br>You need to add the PrometheusFilter in you Filter class:
 * <pre><code>
 * {@literal @}Singleton
 * public class Filters extends DefaultHttpFilters {
 *
 *    {@literal @}Inject
 *     public Filters(LoggingFilter logging, PrometheusFilter prometheus) {
 *       super(logging, prometheus);
 *     }
 *  }
 * </code></pre>
 * <p>
 * This filter will load the following collectors:
 * <ol>
 * <li>http_requests_total (counter) - labelled by:</li>
 *  <ul>
 *      <li>method (get/post/etc)</li>
 *      <li>status code (200, 400, ...)</li>
 *  </ul>
 * <li>http_request_duration_seconds (histogram)</li>
 * <li>http_in_flight_requests_total (gauge)</li>
 * </ol>
 *
 * @author Daniel Ochoa
 */
public class PrometheusFilter extends Filter {

    private static Counter requestsTotal = Counter.build()
            .name("http_requests_total").help("Number of requests")
            .labelNames("method", "status_code").register();

    private static Histogram requestDurationSeconds = Histogram.build()
            .name("http_request_duration_seconds").help("Duration of a request").register();

    private static Gauge inFlightRequestsTotal = Gauge.build()
            .name("http_in_flight_requests_total").help("Number of in-flight requests").register();


    @Inject
    public PrometheusFilter(Materializer mat) {
        super(mat);
    }

    @Override
    public CompletionStage<Result> apply(
            Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
            Http.RequestHeader requestHeader) {

        inFlightRequestsTotal.inc();
        Histogram.Timer requestTimer = requestDurationSeconds.startTimer();

        return nextFilter.apply(requestHeader).thenApply(result -> {
            requestsTotal.labels(requestHeader.method(), String.valueOf(result.status())).inc();
            inFlightRequestsTotal.dec();
            requestTimer.observeDuration();
            return result;
        });
    }
}
