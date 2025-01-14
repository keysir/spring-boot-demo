package com.git.hui.demo.prometheus.metric;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yihui
 * @date 2021/4/19
 */
@Aspect
@Component
public class MetricAspect {
    @Pointcut("execution(public * com.git.hui.demo.prometheus.service.*.*(..))")
    public void point() {
    }

    AtomicInteger ato = new AtomicInteger(0);


    /**
     * qps : irate(case_server_counter_total[10s])
     * rt : irate(case_server_duration_seconds_sum[10s]) / irate(case_server_duration_seconds_count[10s])
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("point()")
    public Object exec(ProceedingJoinPoint joinPoint) throws Throwable {
        String service = joinPoint.getTarget().getClass().getSimpleName();
        String method = joinPoint.getSignature().getName();
        int code = 0;

        MetricWrapper.create().key("metric.service.count").tag(Tag.of("service", service))
                .tag(Tag.of("method", method))
                .tag(Tag.of("code", String.valueOf(code)))
                .gauge(ato);
        Timer.Sample sample = Timer.start();
        try {
            ato.addAndGet(1);
            return joinPoint.proceed();
        } catch (Exception e) {
            code = 500;
            throw e;
        } finally {
            ato.addAndGet(-1);
            // 计数 +1
            MetricWrapper.create().key("metric.service.count").tag(Tag.of("service", service))
                    .tag(Tag.of("method", method))
                    .tag(Tag.of("code", String.valueOf(code)))
                    .count().increment();

            // 统计采样
            sample.stop(MetricWrapper.create().key("metric.service.duration").tag(Tag.of("service", service))
                    .tag(Tag.of("method", method))
                    .tag(Tag.of("code", String.valueOf(code)))
                    .min(1L)
                    .max(100_000L)
                    .slots(100L)
                    .slots(200L)
                    .slots(400L)
                    .slots(800L)
                    .slots(1600L)
                    .slots(3200L)
                    .slots(100_000L)
                    .histogram());
        }
    }
}
