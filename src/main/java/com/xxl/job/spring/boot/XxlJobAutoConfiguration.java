package com.xxl.job.spring.boot;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.spring.boot.metrics.MetricMethodJobHandler;
import com.xxl.job.spring.boot.metrics.XxlJobMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(XxlJobExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@Import(ScheduleJobBeanDefinitionRegistrar.class)
@AllArgsConstructor
public class XxlJobAutoConfiguration {

    private Collection<Tag> tags;

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = XxlJobProperties.PREFIX, name = "metrics.enable", havingValue = "true")
    public JobHandlerWrapper metricsJobHandlerWrapper(ObjectProvider<MeterRegistry> registryProvider,
                                                      XxlJobProperties properties) {

        log.info(">>>>>>>>>>> xxl-job auto binding and metrics executor init.");

        final Collection<Tag> extraTags = properties.getMetrics()
                .getExtraTags()
                .entrySet()
                .stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        extraTags.add(Tag.of("executor", properties.getAppName()));

        this.tags = extraTags;

        return (jobHandler, context) -> new MetricMethodJobHandler(
                registryProvider.getObject(), context, jobHandler, this.tags);

    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = XxlJobProperties.PREFIX, name = "metrics.enable", havingValue = "true")
    public XxlJobMetrics xxlJobMetrics() {
        return new XxlJobMetrics(this.tags);
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoEnrolledXxlJobExecutor xxlJobExecutor(ObjectProvider<OkHttpClient> okhttp3ClientProvider, XxlJobProperties properties,
                                                     ObjectProvider<List<JobHandlerWrapper>> wrapperProviders,
                                                     ObjectProvider<ObjectMapper> objectMapperObjectProvider) {
        return new AutoEnrolledXxlJobExecutor(okhttp3ClientProvider, properties, wrapperProviders, objectMapperObjectProvider);
    }
}