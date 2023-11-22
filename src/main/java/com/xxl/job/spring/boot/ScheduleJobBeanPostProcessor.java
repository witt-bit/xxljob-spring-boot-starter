package com.xxl.job.spring.boot;

import com.xxl.job.spring.boot.annotation.ScheduleJob;
import com.xxl.job.spring.boot.annotation.support.ScheduleJobContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>{@link ScheduleJob}bean后置处理器</p>
 * <p>创建于 2023/11/20 下午6:37 </p>
 *
 * @author <a href="mailto:fgwang.660@gmail.com">witt</a>
 * @version v1.0
 * @since 2.0.0
 */
public class ScheduleJobBeanPostProcessor implements BeanPostProcessor {

    private final AutoEnrolledXxlJobExecutor xxlJobSpringExecutor;

    public ScheduleJobBeanPostProcessor(AutoEnrolledXxlJobExecutor xxlJobSpringExecutor) {
        this.xxlJobSpringExecutor = xxlJobSpringExecutor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        final Map<Method, ScheduleJob> annotatedMethods =
                MethodIntrospector.selectMethods(targetClass, (MethodIntrospector.MetadataLookup<ScheduleJob>)
                        method -> AnnotatedElementUtils.findMergedAnnotation(method, ScheduleJob.class));

        if (annotatedMethods.isEmpty()) {
            return bean;
        }

        // 异步创建组
        for (final Map.Entry<Method, ScheduleJob> annotatedMethod : annotatedMethods.entrySet()) {

            final ScheduleJobContext context
                    = new ScheduleJobContext(bean, annotatedMethod.getKey(), annotatedMethod.getValue());

            this.xxlJobSpringExecutor.registerJobHandler(context);
        }

        return bean;
    }
}