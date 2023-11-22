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
package com.xxl.job.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import com.xxl.job.spring.boot.annotation.ScheduleJob;
import com.xxl.job.spring.boot.annotation.support.ScheduleJobContext;
import com.xxl.job.spring.boot.cookie.CaffeineCacheCookieJar;
import com.xxl.job.spring.boot.model.XxlJobGroup;
import com.xxl.job.spring.boot.model.XxlJobGroupList;
import com.xxl.job.spring.boot.model.XxlJobInfo;
import com.xxl.job.spring.boot.model.XxlJobInfoList;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Xxl Job Handler 自动注册
 *
 * @author ： <a href="https://github.com/hiwepy">wandl</a>
 */
public class AutoEnrolledXxlJobExecutor extends XxlJobSpringExecutor {

    private static final Logger log = LoggerFactory.getLogger("xxlJobStarter");

    private final XxlJobTemplate xxlJobTemplate;

    /**
     * 配置属性
     */
    private final XxlJobProperties properties;

    /**
     * 包装器提供程序
     */
    private final ObjectProvider<List<JobHandlerWrapper>> wrapperProviders;

    /**
     * xxl作业组id
     */
    private volatile Integer xxlJobGroupId;

    /**
     * xxl作业组注册锁
     */
    private CountDownLatch xxlJobGroupEnrollLatch = new CountDownLatch(1);

    /**
     * 自动注册作业执行器
     */
    private ExecutorService autoEnrolledJobExecutor;

    private final List<CompletableFuture<?>> jobFutures = new ArrayList<>(8);

    /**
     * 任务组创建的{@link CompletableFuture}对象
     */
    private volatile CompletableFuture<?> jobGroupCreatedFuture;

    public AutoEnrolledXxlJobExecutor(ObjectProvider<OkHttpClient> okhttp3ClientProvider, XxlJobProperties properties,
                                      ObjectProvider<List<JobHandlerWrapper>> wrapperProviders,
                                      ObjectProvider<ObjectMapper> objectMapperObjectProvider) {

        final OkHttpClient okhttp3Client = okhttp3ClientProvider
                .getIfAvailable(() -> new OkHttpClient.Builder()
                        .cookieJar(new CaffeineCacheCookieJar(properties.getMaximumSize(),
                                properties.getExpiredCookieAfterWrite(),
                                properties.getExpiredCookieAfterAccess()))
                        .build());
        this.xxlJobTemplate = new XxlJobTemplate(okhttp3Client, properties, objectMapperObjectProvider);
        this.properties = properties;
        this.wrapperProviders = wrapperProviders;
        this.autoEnrolledJobExecutor = Executors.newFixedThreadPool(51);

        this.bindProperties();
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (null == this.xxlJobGroupId || null == this.jobGroupCreatedFuture || this.jobFutures.isEmpty()) {
            log.warn("xxlJob annotation JobHandler not found , exit xxlJob initialize .");
            this.clearUp();
            return;
        }

        // 注册时间
        final Duration waitAutoEnrollTime = this.properties.getWaitAutoEnrollTime();

        try {
            this.jobGroupCreatedFuture.get(waitAutoEnrollTime.toNanos(), TimeUnit.NANOSECONDS);
            CompletableFuture.allOf(this.jobFutures.toArray(new CompletableFuture[0]))
                    .get(waitAutoEnrollTime.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("create xxlJob Group or xxl Job fail !", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("After waiting for the xxlJob auto-registration for 2m," +
                    " please check whether the xxlJob service is available !", e);
        }


        // refresh GlueFactory
        GlueFactory.refreshInstance(1);

        // super start
        try {
            this.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("xxlJob start success.");
        this.clearUp();
    }

    /**
     * 清理
     */
    private void clearUp() {
        log.debug("xxlJob clean objects .");
        // 关闭xxlJob Task初始化
        this.autoEnrolledJobExecutor.shutdown();

        // 释放所有缓存对象，被JVM回收
        this.autoEnrolledJobExecutor = null;
        this.xxlJobGroupEnrollLatch = null;
        this.jobGroupCreatedFuture = null;
        this.xxlJobGroupId = null;
        this.jobFutures.clear();
    }

    /**
     * 注册作业处理程序
     *
     * @param context 上下文
     * @see #registJobHandler(XxlJob, Object, Method)
     */
    public void registerJobHandler(ScheduleJobContext context) {
        // 异步创建组
        this.enrollOrGetJobGroup();
        // 异步创建Job
        jobFutures.add(CompletableFuture.runAsync(() -> this.doRegisterJobHandler(context), this.autoEnrolledJobExecutor));
    }

    /**
     * 注册作业处理程序
     *
     * @param context 上下文
     * @see #registJobHandler(XxlJob, Object, Method)
     */
    public void doRegisterJobHandler(ScheduleJobContext context) {

        this.autoEnrollJobHandler(context);

        final Object bean = context.getBean();
        final Method executeMethod = context.getMethod();


        //make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String jobName = context.getId();

        if (loadJobHandler(jobName) != null) {
            throw new RuntimeException("xxl-job jobHandler[" + jobName + "] naming conflicts.");
        }

        final Method initMethod = this.getMethodAndMakeAccessible(clazz, context.getAnnotation().init());
        executeMethod.setAccessible(true);

        final Method destroyMethod = this.getMethodAndMakeAccessible(clazz, context.getAnnotation().destroy());
        executeMethod.setAccessible(true);

        final IJobHandler jobHandler = new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod);
        // registry jobHandler
        registJobHandler(jobName, this.applyJobHandlerWrapper(context, jobHandler));
    }


    /**
     * 应用作业处理程序包装
     *
     * @param jobHandler 作业处理程序
     * @param context    上下文
     * @return {@link IJobHandler}
     */
    protected IJobHandler applyJobHandlerWrapper(ScheduleJobContext context, IJobHandler jobHandler) {
        final List<JobHandlerWrapper> wrappers = wrapperProviders.getIfAvailable();
        if (null == wrappers) {
            return jobHandler;
        }

        IJobHandler wrappedHandler = jobHandler;
        for (JobHandlerWrapper jobHandlerWrapper : wrappers) {
            wrappedHandler = jobHandlerWrapper.wrap(wrappedHandler, context);
        }

        return wrappedHandler;
    }

    /**
     * 自动注册作业处理程序
     */
    private void autoEnrollJobHandler(ScheduleJobContext scheduleJobContext) {
        // 等待组创建结束
        try {
            this.xxlJobGroupEnrollLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 定时任务是否存在
        final XxlJobInfoList jobInfoList = this.xxlJobTemplate.jobInfoList(0, Integer.MAX_VALUE, this.xxlJobGroupId);

        final Map<String, XxlJobInfo> groupedJobInfo = Optional.ofNullable(jobInfoList)
                .map(XxlJobInfoList::getData)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .collect(Collectors.toMap(XxlJobInfo::getExecutorHandler, s -> s, (a, b) -> b));

        // 执行器存在或者创建成功，添加定时任务
        final XxlJobInfo xxlJobInfo = this.buildXxlJobInfo(scheduleJobContext);
        xxlJobInfo.setJobGroup(this.xxlJobGroupId);

        // 自动注册
        if (!groupedJobInfo.containsKey(xxlJobInfo.getExecutorHandler())) {
            // 自动添加定时任务
            int jobId = this.xxlJobTemplate.addJob(xxlJobInfo);
            xxlJobInfo.setId(jobId);

            // 如果是自启动，则启动任务
            if (xxlJobInfo.enableAutoStart()) {
                this.xxlJobTemplate.startJob(xxlJobInfo.getId());
            }
            log.info("xxl-job handler '{}' auto register .", scheduleJobContext);
        } else if (this.properties.isForceUpdateJob()) {
            xxlJobInfo.setId(groupedJobInfo.get(xxlJobInfo.getExecutorHandler()).getId());

            log.info("xxl-job handler '{}' force Update .", scheduleJobContext);

            this.xxlJobTemplate.updateJob(xxlJobInfo);

            // 如果是自启动，则启动任务
            if (xxlJobInfo.enableAutoStart()) {
                this.xxlJobTemplate.startJob(xxlJobInfo.getId());
            }
        }
    }

    /**
     * 注册或获取工作组
     */
    private void enrollOrGetJobGroup() {
        if (null != this.jobGroupCreatedFuture) {
            return;
        }
        synchronized (this) {
            if (null != this.jobGroupCreatedFuture) {
                return;
            }
            this.jobGroupCreatedFuture = CompletableFuture.runAsync(this::autoEnrollOrGetJobGroup, this.autoEnrolledJobExecutor);
        }
    }

    /**
     * 自动注册或获取作业组
     */
    private void autoEnrollOrGetJobGroup() {
        if (null != this.xxlJobGroupId) {
            return;
        }

        final String appName = this.properties.getAppName();

        // 检查任务组是否存在
        final XxlJobGroupList jobGroupList = this.xxlJobTemplate.jobInfoGroupList(0, Integer.MAX_VALUE, appName, null);

        final Optional<Integer> jobGroupOpt = Optional.ofNullable(jobGroupList)
                .map(XxlJobGroupList::getData)
                .flatMap(s -> s.stream().filter(xxlJobGroup -> xxlJobGroup.getAppName().equals(appName)).findFirst())
                .map(XxlJobGroup::getId);

        if (jobGroupOpt.isPresent()) {
            this.xxlJobGroupId = jobGroupOpt
                    // 疑似多项目同时启动，导致组创建失败
                    .orElseThrow(() -> new IllegalStateException("xxl-job group not found for " + appName));

            if (null == this.xxlJobGroupId) {
                throw new IllegalStateException("xxl-job group '" + this.properties.getAppName() + "' query fail ! groupId is null .");
            }

            // 组创建结束
            this.xxlJobGroupEnrollLatch.countDown();
            return;
        }

        synchronized (this) {
            if (null != this.xxlJobGroupId) {
                return;
            }

            // 执行器不存在则创建
            log.info("xxl-job group '{}' auto Creating ...", appName);
            // 创建任务组对象
            final XxlJobGroup xxlJobGroup = new XxlJobGroup();
            xxlJobGroup.setAppName(appName);
            xxlJobGroup.setAddressType(0);
            xxlJobGroup.setOrder(Optional.ofNullable(jobGroupList)
                    .map(XxlJobGroupList::getRecordsTotal)
                    .orElse(0) + 1);
            xxlJobGroup.setTitle(this.properties.getAppTitle());
            this.xxlJobTemplate.addJobGroup(xxlJobGroup);

            this.autoEnrollOrGetJobGroup();
        }
    }

    /**
     * 绑定属性
     */
    private void bindProperties() {
        this.setAdminAddresses(this.properties.getAddresses());
        this.setAppname(this.properties.getAppName());
        this.setIp(this.properties.getIp());
        this.setPort(this.properties.getPort());
        this.setAccessToken(this.properties.getAccessToken());
        this.setLogPath(this.properties.getLogPath());
        this.setLogRetentionDays(this.properties.getLogRetentionDays());
    }

    private XxlJobInfo buildXxlJobInfo(ScheduleJobContext scheduleJobContext) {
        final ScheduleJob annotation = scheduleJobContext.getAnnotation();
        final XxlJobInfo xxlJobInfo = new XxlJobInfo();

        // 任务描述
        xxlJobInfo.setJobDesc(annotation.name());
        // 负责人
        xxlJobInfo.setAuthor(annotation.author());
        // 报警邮件
        xxlJobInfo.setAlarmEmail(StringUtils.arrayToCommaDelimitedString(annotation.emails()));
        // 调度类型
        xxlJobInfo.setScheduleType(annotation.scheduleType().name());
        // Cron
        xxlJobInfo.setScheduleConf(annotation.schedule());
        xxlJobInfo.setJobCron(annotation.schedule());
        // 运行模式
        xxlJobInfo.setGlueType(annotation.glueType().name());
        // JobHandler
        xxlJobInfo.setExecutorHandler(scheduleJobContext.getId());
        // 任务参数
        xxlJobInfo.setExecutorParam(annotation.param());
        // 路由策略
        xxlJobInfo.setExecutorRouteStrategy(annotation.routeStrategy().name());
        // 失败重试次数
        xxlJobInfo.setExecutorFailRetryCount(annotation.failRetryCount());
        // 调度过期策略
        xxlJobInfo.setMisfireStrategy(annotation.misfireStrategy().name());
        // 阻塞处理策略
        xxlJobInfo.setExecutorBlockStrategy(annotation.blockStrategy().name());
        // 任务超时时间
        xxlJobInfo.setExecutorTimeout(annotation.timeout());
        // 是否自启动
        xxlJobInfo.autoStart(annotation.autoStart());
        return xxlJobInfo;
    }

    private Method getMethodAndMakeAccessible(Class<?> clazz, String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        final Method destroyMethod = ReflectionUtils.findMethod(clazz, name);
        if (null == destroyMethod) {
            return null;
        }
        ReflectionUtils.makeAccessible(destroyMethod);

        return destroyMethod;
    }
}