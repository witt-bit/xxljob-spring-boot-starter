package com.xxl.job.spring.boot;

import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.spring.boot.annotation.support.ScheduleJobContext;

/**
 * <p>{@link com.xxl.job.core.handler.IJobHandler}包装器</p>
 * <p>创建于 2023/11/20 下午5:36 </p>
 *
 * @author <a href="mailto:fgwang.660@gmail.com">witt</a>
 * @version v1.0
 * @since 2.0.0
 */
@FunctionalInterface
public interface JobHandlerWrapper {

    /**
     * 包
     *
     * @param jobHandler 作业处理程序
     * @param context    上下文
     * @return {@link IJobHandler}
     */
    IJobHandler wrap(IJobHandler jobHandler, ScheduleJobContext context);
}