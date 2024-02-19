package com.xxl.job.spring.boot;

import com.xxl.job.spring.boot.annotation.ScheduleJob;
import com.xxl.job.spring.boot.annotation.support.ScheduleJobContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
            final Method method = annotatedMethod.getKey();
            final ScheduleJob annotation = annotatedMethod.getValue();
            this.checkAnnotationMethod(method);
            final ScheduleJobContext context = new ScheduleJobContext(bean, method, annotation);
            this.xxlJobSpringExecutor.registerJobHandler(context);
        }

        return bean;
    }

    /**
     * 对注解的方法进行检查
     *
     * @param method 方法
     */
    private void checkAnnotationMethod(Method method) {
        final int modifiers = method.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            throw new IllegalStateException("@ScheduleJob method '" + method + "' cannot use 'private' modifier !");
        }

        if (Modifier.isStatic(modifiers)) {
            throw new IllegalStateException("@ScheduleJob method '" + method + "' cannot use 'static' modifier !");
        }

        if (Modifier.isFinal(modifiers)) {
            throw new IllegalStateException("@ScheduleJob method '" + method + "' cannot use 'final' modifier !");
        }
    }
}