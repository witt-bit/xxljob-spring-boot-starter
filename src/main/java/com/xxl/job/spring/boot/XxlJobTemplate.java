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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.spring.boot.model.XxlJobGroup;
import com.xxl.job.spring.boot.model.XxlJobGroupList;
import com.xxl.job.spring.boot.model.XxlJobInfo;
import com.xxl.job.spring.boot.model.XxlJobInfoList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class XxlJobTemplate {

    protected OkHttpClient okhttp3Client;

    protected XxlJobProperties properties;

    protected ObjectMapper objectMapper;

    public XxlJobTemplate(OkHttpClient okhttp3Client, XxlJobProperties properties,
                          ObjectProvider<ObjectMapper> objectMapperObjectProvider) {
        this.okhttp3Client = okhttp3Client;
        this.properties = properties;
        this.objectMapper = objectMapperObjectProvider.getIfAvailable(ObjectMapper::new);
    }

    public void login(String userName, String password, boolean remember) {
        // xxl-job admin 请求参数
        final Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("userName", userName);
        paramMap.put("password", password);
        paramMap.put("ifRemember", remember ? "on" : "off");
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.LOGIN_GET);
        Request request = this.buildRequestEntity(url, paramMap, true);
        this.doRequest(request, response -> this.parseResponseEntity(response, false,
                new TypeReference<ReturnT<Map<String, Object>>>() {
                }));
    }

    /**
     * 退出登录
     */
    public void logout() {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = Collections.emptyMap();
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.LOGOUT_GET);
        Request request = this.buildRequestEntity(url, paramMap);
        // xxl-job admin 请求操作
        try (Response response = okhttp3Client.newCall(request).execute()) {
            // xxl-job admin 请求结果成功
            if (response.isSuccessful()) {
                return;
            }
            throw new IllegalStateException("Xxl-job logout fail, respCode: "
                    + response.code() + ",respMsg:" + response.message());
        } catch (IOException e) {
            throw new IllegalStateException("Build xxlJob logout Request fail !", e);
        }
    }

    /**
     * 获取xxl-job 执行器列表数据
     *
     * @param start   起始位置
     * @param length  数量
     * @param appName 执行器名称
     * @param title   执行器标题
     * @return {@link XxlJobGroupList}
     */
    public XxlJobGroupList jobInfoGroupList(int start, int length, String appName, String title) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(7);
        paramMap.put("start", Math.max(0, start));
        paramMap.put("length", Math.min(length, 5));
        paramMap.put("appname", appName);
        paramMap.put("title", title);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBGROUP_PAGELIST);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response, true,
                new TypeReference<XxlJobGroupList>() {
                }));
    }

    /**
     * 获取调度任务组
     *
     * @param jobGroupId 调度任务组ID
     * @return ReturnT
     */
    public XxlJobGroup jobInfoGroup(int jobGroupId) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("id", jobGroupId);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBGROUP_GET);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response, true, new TypeReference<ReturnT<XxlJobGroup>>() {
        }));
    }

    /**
     * 添加调度任务组
     *
     * @param jobGroup 调度任务组信息Model
     */
    public void addJobGroup(XxlJobGroup jobGroup) {
        if (null != jobGroup.getTitle() && jobGroup.getTitle().length() > 12) {
            throw new IllegalArgumentException("jobGroup field 'title' too long , need <= 12 chars .");
        }
        String url = this.joinPath(XxlJobConstants.JOBGROUP_SAVE);
        Request request = this.buildRequestEntity(url, jobGroup, false);
        // xxl-job admin 请求操作
        this.doRequest(request, response -> this.parseResponseEntity(response, false, new TypeReference<ReturnT<String>>() {
        }));
    }

    /**
     * 更新调度任务组
     *
     * @param jobGroup 调度任务组信息Model
     * @return ReturnT
     */
    public String updateJobGroup(XxlJobGroup jobGroup) {
        // xxl-job admin 请求参数
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBGROUP_UPDATE);
        Request request = this.buildRequestEntity(url, jobGroup, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response, true,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 删除调度任务组
     *
     * @param jobGroupId 调度任务组ID
     * @return ReturnT
     */
    public String removeJobGroup(Integer jobGroupId) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("id", jobGroupId);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBGROUP_REMOVE);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response, true,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 获取xxl-job 执行器列表数据
     *
     * @param start    起始位置
     * @param length   数量
     * @param jobGroup 执行器主键ID
     * @return {@link XxlJobInfoList}
     */
    public XxlJobInfoList jobInfoList(int start, int length, Integer jobGroup) {
        return this.jobInfoList(start, length, jobGroup, -1, null, null, null);
    }

    /**
     * 获取xxl-job 执行器列表数据
     *
     * @param start         起始位置
     * @param length        数量
     * @param jobGroup      执行器主键ID
     * @param triggerStatus 调度状态：0-停止，1-运行
     * @return {@link XxlJobInfoList}
     */
    public XxlJobInfoList jobInfoList(int start, int length, Integer jobGroup, Integer triggerStatus) {
        return this.jobInfoList(start, length, jobGroup, triggerStatus, "", "", "");
    }

    /**
     * 获取xxl-job 执行器列表数据
     *
     * @param start           起始位置
     * @param length          数量
     * @param jobGroup        执行器主键ID
     * @param triggerStatus   调度状态：0-停止，1-运行
     * @param jobDesc         任务描述
     * @param executorHandler 执行器任务handler
     * @param author          任务创建者
     * @return {@link XxlJobInfoList}
     */
    public XxlJobInfoList jobInfoList(int start, int length, Integer jobGroup,
                                      Integer triggerStatus, String jobDesc, String executorHandler, String author) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(7);
        paramMap.put("start", Math.max(0, start));
        paramMap.put("length", Math.max(length, 5));
        paramMap.put("jobGroup", jobGroup);
        paramMap.put("triggerStatus", triggerStatus);
        paramMap.put("jobDesc", jobDesc);
        paramMap.put("executorHandler", executorHandler);
        paramMap.put("author", author);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_PAGELIST);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response,
                true,
                new TypeReference<XxlJobInfoList>() {
                }));
    }

    /**
     * 新增调度任务
     *
     * @param jobInfo 调用任务信息Model
     * @return 任务id
     */
    public int addJob(XxlJobInfo jobInfo) {
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_ADD);
        Request request = this.buildRequestEntity(url, jobInfo, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, resp -> this.parseResponseEntity(resp, true,
                new TypeReference<ReturnT<Integer>>() {
                }));
    }

    /**
     * 修改调度任务
     *
     * @param jobInfo 调用任务信息Model
     * @return {@link String}
     */
    public String updateJob(XxlJobInfo jobInfo) {
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_UPDATE);
        Request request = this.buildRequestEntity(url, jobInfo, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response,
                true,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 删除调度任务
     *
     * @param jobId 任务id
     * @return {@link String}
     */
    public String removeJob(Integer jobId) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("id", jobId);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_REMOVE);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response,
                true,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 停止调度
     *
     * @param jobId 任务id
     */
    public void stopJob(Integer jobId) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("id", jobId);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_STOP);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        this.doRequest(request, response -> this.parseResponseEntity(response,
                false,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 开启调度
     *
     * @param jobId 任务id
     */
    public void startJob(Integer jobId) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("id", jobId);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_START);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        this.doRequest(request, response -> this.parseResponseEntity(response,
                false,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    /**
     * 手动触发一次调度
     *
     * @param jobInfo 调用任务信息Model
     * @return ReturnT
     */
    public String triggerJob(XxlJobInfo jobInfo) {
        return this.triggerJob(jobInfo.getId(), jobInfo.getExecutorParam());
    }

    /**
     * 手动触发一次调度
     *
     * @param jobInfoId     调用任务ID
     * @param executorParam 执行器参数
     * @return ReturnT
     */
    public String triggerJob(Integer jobInfoId, String executorParam) {
        // xxl-job admin 请求参数
        Map<String, Object> paramMap = new HashMap<>(2);
        paramMap.put("id", jobInfoId);
        paramMap.put("executorParam", executorParam);
        // xxl-job admin 请求体
        String url = this.joinPath(XxlJobConstants.JOBINFO_TRIGGER);
        Request request = this.buildRequestEntity(url, paramMap, false);
        // xxl-job admin 请求操作
        return this.doRequest(request, response -> this.parseResponseEntity(response,
                true,
                new TypeReference<ReturnT<String>>() {
                }));
    }

    private Request buildRequestEntity(String url, Map<String, Object> paramMap) {
        return this.buildRequestEntity(url, paramMap, false);
    }

    private Request buildRequestEntity(String url, Object paramObject, boolean isLoginRequest) {

        // xxl-job admin 请求头
        Headers.Builder headers = new Headers.Builder()
                .add(XxlJobConstants.XXL_RPC_ACCESS_TOKEN, properties.getAccessToken());

        // xxl-job admin 请求体

        @SuppressWarnings("unchecked") final Map<String, Object> paramMap = paramObject instanceof Map ? (Map<String, Object>) paramObject :
                this.objectMapper.convertValue(paramObject, new TypeReference<Map<String, Object>>() {
                });
        // 创建一个RequestBody(参数1：数据类型 参数2传递的json串)
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : paramMap.keySet()) {
            Object obj = paramMap.get(key);
            if (obj != null) {
                builder.addEncoded(key, paramMap.get(key).toString());
            }
        }
        FormBody requestBody = builder.build();

        // 创建一个请求对象
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (null == httpUrl) {
            throw new IllegalArgumentException("httpUrl is required !");
        }

        Request.Builder request = new Request.Builder().url(httpUrl).headers(headers.build()).post(requestBody);

        // 非登录请求需要检查登录状态
        if (!isLoginRequest) {
            this.loginIfNeed(httpUrl, headers, request);
        }
        return request.build();
    }

    private synchronized void loginIfNeed(HttpUrl httpUrl, Headers.Builder headers, Request.Builder request) {
        // xxl-job admin cookie
        CookieJar cookieJar = okhttp3Client.cookieJar();
        List<Cookie> cookies = cookieJar.loadForRequest(httpUrl);
        // 缓存中的 cookie 不为空，查找我们需要的 cookie
        if (CollectionUtils.isEmpty(cookies) || cookies.stream().noneMatch(cookie -> XxlJobConstants.XXL_RPC_COOKIE.equals(cookie.name()))) {
            log.info("Login Xxl-job ...");
            // 缓存中的 cookie 为空，或者缓存中的 cookie 不包含我们需要的 cookie
            this.login(properties.getUsername(), properties.getPassword(), properties.isRemember());
        }
    }

    private <T> T doRequest(Request request, Function<Response, T> respParser) {
        // xxl-job admin 请求操作
        try {
            // 发送请求获取响应
            final Response response = okhttp3Client.newCall(request).execute();
            // 请求结果处理
            return respParser.apply(response);
        } catch (IOException e) {
            throw new IllegalStateException("Build xxl-job request fail !");
        }
    }

    @SuppressWarnings("unchecked")
    private <T, R> R parseResponseEntity(Response response, boolean requiredBody, TypeReference<T> typeRef) {
        try {
            String url = response.request().url().toString();
            // xxl-job admin 请求结果成功
            if (response.isSuccessful()) {
                final ResponseBody responseBody = response.body();
                T result = null;
                if (null != responseBody) {
                    final String body = responseBody.string();
                    result = this.objectMapper.readValue(body, typeRef);
                    if (result instanceof ReturnT<?>) {
                        result = this.getResult((ReturnT<? extends T>) result);
                    }
                }
                if (requiredBody && null == result) {
                    throw new IllegalStateException("Xxl-job server '" + url + "' fail, body is Required, respCode: "
                            + response.code() + ",respMsg:" + response.message());
                }
                return (R) result;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Parse xxl-Job response fail , respCode: "
                    + response.code() + ",respMsg:" + response.message(), e);
        }

        throw new IllegalStateException("Xxl-job server fail, respCode: "
                + response.code() + ",respMsg:" + response.message());
    }

    /*
     * 字符串拼接
     *
     * @param suffix
     * @return
     */
    private String joinPath(String suffix) {
        final URI schema = URI.create(properties.getAddresses());
        return schema.resolve(schema.getPath() + suffix).normalize().toString();
    }

    private <R> R getResult(ReturnT<R> result) {
        final int code = result.getCode();
        if (code == ReturnT.SUCCESS_CODE) {
            return result.getContent();
        }

        // language=JSON
        String respString = "{ \"code\": " + code + ", \"msg\": " + result.getMsg() + " }";
        throw new IllegalStateException("Xxl-job server reject : '" + respString + "' !");
    }

}