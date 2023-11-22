/*
 * Copyright (c) 2017, hiwepy (https://github.com/hiwepy).
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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(XxlJobProperties.PREFIX)
@Getter
@Setter
@ToString
public class XxlJobProperties {

    public static final String PREFIX = "xxl-job";

    /**
     * 执行器通讯TOKEN [选填]：非空时启用；
     */
    private String accessToken;


    /**
     * 调度中心部署跟地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔 。
     * 执行器将会使用该地址进行"执行器心跳注册"和"任务结果回调"；为空则关闭自动注册；
     */
    private String addresses;

    /**
     * 调度中心登录账号
     */
    private String username;

    /**
     * 调度中心登录密码
     */
    private String password;

    /**
     * 调度中心登录状态保持，开启后xxl-job登录状态不过期，默认：2H
     */
    private boolean remember = true;

    /**
     * he maximum size of the cache
     */
    private long maximumSize = 10_000;

    /**
     * the length of time after an entry is created that it should be automatically removed
     */
    private Duration expiredCookieAfterWrite = Duration.ofMinutes(30);

    /**
     * the length of time after an entry is created that it should be automatically removed
     */
    private Duration expiredCookieAfterAccess = Duration.ofMinutes(30);

    /**
     * 执行器AppName [必填]：执行器心跳注册分组依据；为空则关闭自动注册.
     */
    private String appName = "${spring.application.name}";

    /**
     * 标题
     */
    private String appTitle = this.appName + "jobExecutor";

    /**
     * 执行器IP [选填]：默认为空表示自动获取IP，多网卡时可手动设置指定IP,
     * 该IP不会绑定Host仅作为通讯实用；地址信息用于 "执行器注册" 和 "调度中心请求并触发任务"；
     */
    private String ip;

    /**
     * 执行器端口号 [选填]：小于等于0则自动获取；默认端口为9999，单机部署多个执行器时，注意要配置不同执行器端口；
     */
    private int port = -1;

    /**
     * 执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径；
     */
    private String logPath = "/opt/logs/xxl-job";

    /**
     * 执行器日志保存天数 [选填] ：值大于3时生效，启用执行器Log文件定期清理功能，否则不生效；
     */
    private int logRetentionDays = 30;

    /**
     * 执行器，任务Handler名称
     */
    private String defaultExecutorHandler = XxlJobConstants.DEFAULT_HTTP_JOB_HANDLER;

    /**
     * 项目启动后，会以代码中最新的注解内容更新xxl后台配置内容，这种方式会造成数据覆盖，默认情况下，只允许新建，仍然以后台配置的为准
     */
    private boolean forceUpdateJob = false;

    /**
     * 等待自动注册时间
     */
    private Duration waitAutoEnrollTime = Duration.ofSeconds(5);

    /**
     * 监控
     */
    private final Metrics metrics = new Metrics();

    /**
     * 监控
     *
     * @author witt
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Metrics {

        /**
         * Whether Enable Xxl Job Metrics.
         */
        private boolean enable = false;

        /**
         * Extra tags for metrics.
         */
        private Map<String, String> extraTags = new LinkedHashMap<>(16);
    }
}