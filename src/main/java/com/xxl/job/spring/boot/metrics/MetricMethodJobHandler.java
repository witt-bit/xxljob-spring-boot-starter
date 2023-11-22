package com.xxl.job.spring.boot.metrics;

import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.spring.boot.annotation.support.ScheduleJobContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MetricMethodJobHandler
 */
@Slf4j
public class MetricMethodJobHandler extends IJobHandler {
    private final MeterRegistry registry;

    private final Collection<Tag> tags;

    private final Counter submitted;

    private final Counter running;

    private final Counter completed;

    private final Timer duration;

    private final ScheduleJobContext context;

    private final IJobHandler jobHandler;

    public MetricMethodJobHandler(MeterRegistry registry, ScheduleJobContext context,
                                  IJobHandler jobHandler,
                                  Collection<Tag> tags) {

        this.registry = registry;
        this.context = context;
        this.jobHandler = jobHandler;

        this.tags = Objects.isNull(tags) ? Collections.emptyList() : tags;
        this.submitted = registry.counter(XxlJobMetrics.METRIC_NAME_JOB_REQUESTS_SUBMITTED, this.tags);
        this.running = registry.counter(XxlJobMetrics.METRIC_NAME_JOB_REQUESTS_RUNNING, this.tags);
        this.completed = registry.counter(XxlJobMetrics.METRIC_NAME_JOB_REQUESTS_COMPLETED, this.tags);
        this.duration = registry.timer(XxlJobMetrics.METRIC_NAME_JOB_REQUESTS_DURATION, this.tags);

    }

    @Override
    public void execute() throws Exception {

        String id = this.context.getId();

        // 1、创建并启动 StopWatch
        StopWatch stopWatch = new StopWatch(id);


        // 一次请求计数 +1
        submitted.increment();
        // 当前正在运行的请求数 +1
        running.increment();

        // 3、获取 XxlJobCron 注解
        String metric = MetricNames.name(XxlJobMetrics.XXL_JOB_METRIC_NAME_PREFIX, id);
        List<Tag> jobTags = new ArrayList<>(tags);
        jobTags.add(Tag.of("job", id));
        Timer timer = registry.timer(metric, jobTags);

        stopWatch.start(id);
        try {
            this.jobHandler.execute();
        } finally {
            stopWatch.stop();
            // 记录本次请求耗时
            timer.record(stopWatch.getTotalTimeMillis(), TimeUnit.MILLISECONDS);
            duration.record(stopWatch.getTotalTimeMillis(), TimeUnit.MILLISECONDS);
            // 当前正在运行的请求数 -1
            running.increment(-1);
            // 当前已完成的请求数 +1
            completed.increment();
            // 2、记录方法执行时间
            log.info(stopWatch.prettyPrint());
        }

    }

    @Override
    public void init() throws Exception {
        this.jobHandler.init();
    }

    @Override
    public void destroy() throws Exception {
        this.jobHandler.destroy();
    }

    @Override
    public String toString() {
        return this.jobHandler.toString();
    }
}