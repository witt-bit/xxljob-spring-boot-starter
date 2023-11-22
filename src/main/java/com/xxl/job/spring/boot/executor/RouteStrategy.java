package com.xxl.job.spring.boot.executor;

import lombok.NoArgsConstructor;

/**
 * 任务执行路由策略枚举类,枚举的变量命名，就是xxlJob需要的值
 */
@NoArgsConstructor
public enum RouteStrategy {

    /**
     * 第一个: FIRST
     */
    FIRST,

    /**
     * 最后一个: LAST
     */
    LAST,

    /**
     * 轮询: ROUND
     */
    ROUND,

    /**
     * 随机: RANDOM
     */
    RANDOM,

    /**
     * 一致性HASH: CONSISTENT_HASH
     */
    CONSISTENT_HASH,

    /**
     * 最不经常使用: LEAST_FREQUENTLY_USED
     */
    LEAST_FREQUENTLY_USED,

    /**
     * 最近最久未使用: LEAST_RECENTLY_USED
     */
    LEAST_RECENTLY_USED,

    /**
     * 故障转移: FAILOVER
     */
    FAILOVER,

    /**
     * 忙碌转移: BUSYOVER
     */
    BUSY_OVER,

    /**
     * 分片广播: SHARDING_BROADCAST
     */
    SHARDING_BROADCAST;

}