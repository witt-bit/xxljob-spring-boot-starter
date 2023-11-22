package com.xxl.job.spring.boot;

import com.xxl.job.spring.boot.annotation.ScheduleJob;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * <p>{@link ScheduleJob}注解Bean注入器</p>
 * <p>创建于 2023/11/21 下午12:24 </p>
 *
 * @author <a href="mailto:fgwang.660@gmail.com">witt</a>
 * @version v1.0
 * @since 2.0.0
 */
@Configuration
@NoArgsConstructor
@AutoConfigureAfter(value = XxlJobAutoConfiguration.class)
public class ScheduleJobBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(ScheduleJobBeanPostProcessor.class.getName())) {
            registry.registerBeanDefinition(ScheduleJobBeanPostProcessor.class.getName(),
                    new RootBeanDefinition(ScheduleJobBeanPostProcessor.class));
        }
    }
}