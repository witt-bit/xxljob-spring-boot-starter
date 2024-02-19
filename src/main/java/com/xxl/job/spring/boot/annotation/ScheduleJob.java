/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.xxl.job.spring.boot.annotation;

import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.spring.boot.executor.MisfireStrategyEnum;
import com.xxl.job.spring.boot.executor.RouteStrategy;
import com.xxl.job.spring.boot.executor.ScheduleType;

import java.lang.annotation.*;

/**
 * 标识这是一个xxl-job任务处理器函数
 *
 * @author witt
 * @see com.xxl.job.core.handler.annotation.XxlJob
 * @see com.xxl.job.spring.boot.ScheduleJobBeanPostProcessor
 * @see com.xxl.job.spring.boot.AutoEnrolledXxlJobExecutor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ScheduleJob {

    /**
     * jobHandler名称,如未填写，取当前方法名的名称，请规范命名{@code xxJobHandler}，注意同服务内不可重复命名
     *
     * @return {@link String}
     */
    String value() default "";

    /**
     * Job描述,中文名称，用于简单描述Job管理内容
     */
    String name();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";

    /**
     * 调度类型 ScheduleTypeEnum
     */
    ScheduleType scheduleType() default ScheduleType.CRON;

    /**
     * 任务调度策略，实际值类型，取决于{@link #scheduleType()}
     */
    String schedule();

    /**
     * 负责人
     */
    String author();

    /**
     * 报警邮件
     */
    String[] emails() default {};

    /**
     * 执行器，任务参数
     */
    String param() default "";

    /**
     * 失败重试次数
     */
    int failRetryCount() default 3;

    /**
     * GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum
     */
    GlueTypeEnum glueType() default GlueTypeEnum.BEAN;

    /**
     * 执行器路由策略
     */
    RouteStrategy routeStrategy() default RouteStrategy.LEAST_FREQUENTLY_USED;

    /**
     * 阻塞处理策略
     */
    ExecutorBlockStrategyEnum blockStrategy() default ExecutorBlockStrategyEnum.DISCARD_LATER;

    /**
     * 调度过期策略
     */
    MisfireStrategyEnum misfireStrategy() default MisfireStrategyEnum.DO_NOTHING;

    /**
     * 任务执行超时时间，单位秒
     */
    int timeout() default 3000;

    /**
     * 自启动,定时任务自动注册后，是否默认启动，如果设置为{@code false},job将永远不会执行
     */
    boolean autoStart() default true;

}