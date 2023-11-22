package com.xxl.job.spring.boot.annotation.support;

import com.xxl.job.spring.boot.annotation.ScheduleJob;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * <p> 调度任务上下文 </p>
 * <p>创建于 2023/11/20 下午2:31 </p>
 *
 * @author <a href="mailto:fgwang.660@gmail.com">witt</a>
 * @version v1.0
 * @since 3.0.0
 */
@Getter
public class ScheduleJobContext implements Serializable {

    private String id;

    private final Object bean;

    private final Method method;

    private final ScheduleJob annotation;

    public ScheduleJobContext(Object bean, Method method, ScheduleJob annotation) {
        this.bean = bean;
        this.method = method;
        this.annotation = annotation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScheduleJobContext that = (ScheduleJobContext) o;

        if (!bean.equals(that.bean)) return false;
        if (!method.equals(that.method)) return false;
        return annotation.equals(that.annotation);
    }

    @Override
    public int hashCode() {
        int result = bean.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + annotation.hashCode();
        return result;
    }

    public String getId() {
        if (null != this.id) {
            return this.id;
        }

        final String value = this.annotation.value();
        if (StringUtils.hasText(value)) {
            this.id = value;
        } else {
            this.id = method.getName();
        }
        return id;
    }

    @Override
    public String toString() {
        return "ScheduleJob{" +
                "id='" + id + '\'' +
                ", bean=" + bean +
                ", method=" + method +
                ", Schedule=" + annotation.scheduleType() + ":" + annotation.schedule() +
                '}';
    }
}