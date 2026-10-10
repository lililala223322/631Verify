/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreHMTask.java
 * 
 * 描述    : 核心健康监控任务配置实体类
 *           对应XML中的 /Module/HealthMonitor/HMTask/CoreHMTaskList/CoreHMTask 节点
 *           按照表14规格实现
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class CoreHMTask {

    /* 成员变量 */
    /* 字段名称 | 数据类型 | 32位占用字节数 | 映射的XML */
    private long fConfigType;                   // 配置类型, unsigned int, 4字节, 固定值 1129468932
    private long fConfigDataSize;               // 配置数据大小, unsigned int, 4字节, 运行时计算
    private char[] fTaskName = new char[32];    // 任务名, 字符数组, 32字节, 不足\0填充
    private char[] fCallbackName = new char[32]; // 回调名称, 字符数组, 32字节, 不足\0填充
    private long fMaxQueueDepth;                // 健康监控事件队列深度, unsigned int, 4字节
    private long fQueueThreshold;               // 健康监控事件队列门限, unsigned int, 4字节
    private long fStackSize;                    // 健康监控任务栈大小, unsigned int, 4字节 (32位)
    private long fMaxLogEntries;                // 最大日志数量, unsigned int, 4字节
    private long fLogEntriesThreshold;          // 日志数量门限, unsigned int, 4字节
    private long fAttributeMask;                // 健康监控属性, unsigned int, 4字节
    private long fEventFilterMask;              // 事件过滤属性, unsigned int, 4字节
    private long fMaxErrorHandlerQueueDepth;    // 核心级健康监控最大错误处理队列深度, unsigned int, 4字节
    private List<Long> fCoreRefIds = new ArrayList<>(); // 配置所属的核心ID数组, unsigned int数组, 255个, 每个4字节
    private long fCoreRefIdsCount;              // 健康监控配置所属的核心ID个数, unsigned int, 4字节
    private long fErrorHandlerQueueThreshold;   // 核心级健康监控错误处理事件队列门限, unsigned int, 4字节

    /**
     * CoreHMTask
     *
     * @brief
     * 功能: 默认构造函数，初始化字符数组并将配置类型设为固定值
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreHMTask() {
        Arrays.fill(fTaskName, (char) 0);
        Arrays.fill(fCallbackName, (char) 0);
        // 默认值：配置类型固定值
        this.fConfigType = 1129468932L;
    }

    /**
     * getfConfigType
     *
     * @brief
     * 功能: 获取配置类型
     * 
     * @return
     * long: 配置类型
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfConfigType() {
        return fConfigType;
    }

    /**
     * setfConfigType
     *
     * @brief
     * 功能: 设置配置类型
     * 
     * @param[in]  fConfigType: 配置类型
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfConfigType(long fConfigType) {
        this.fConfigType = fConfigType;
    }

    /**
     * getfConfigDataSize
     *
     * @brief
     * 功能: 获取配置数据大小
     * 
     * @return
     * long: 配置数据大小
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfConfigDataSize() {
        return fConfigDataSize;
    }

    /**
     * setfConfigDataSize
     *
     * @brief
     * 功能: 设置配置数据大小
     * 
     * @param[in]  fConfigDataSize: 配置数据大小
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfConfigDataSize(long fConfigDataSize) {
        this.fConfigDataSize = fConfigDataSize;
    }

    /**
     * setfTaskName
     *
     * @brief
     * 功能: 以字符串设置任务名，按US_ASCII编码写入字符数组
     * 
     * @param[in]  fTaskName: 任务名字符串，为null时清空字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfTaskName(String fTaskName) {
        if (fTaskName == null) {
            Arrays.fill(this.fTaskName, (char) 0);
            return;
        }
        byte[] bytes = fTaskName.getBytes(StandardCharsets.US_ASCII);
        int length = Math.min(bytes.length, this.fTaskName.length);
        for (int i = 0; i < length; i++) {
            this.fTaskName[i] = (char) bytes[i];
        }
        for (int i = length; i < this.fTaskName.length; i++) {
            this.fTaskName[i] = 0;
        }
    }

    /**
     * getfTaskNameAsString
     *
     * @brief
     * 功能: 将任务名字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 任务名字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfTaskNameAsString() {
        int endIndex = 0;
        while (endIndex < fTaskName.length && fTaskName[endIndex] != 0) {
            endIndex++;
        }
        byte[] bytes = new byte[endIndex];
        for (int i = 0; i < endIndex; i++) {
            bytes[i] = (byte) fTaskName[i];
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * setfCallbackName
     *
     * @brief
     * 功能: 以字符串设置回调名称，按US_ASCII编码写入字符数组
     * 
     * @param[in]  fCallbackName: 回调名称字符串，为null时清空字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCallbackName(String fCallbackName) {
        if (fCallbackName == null) {
            Arrays.fill(this.fCallbackName, (char) 0);
            return;
        }
        byte[] bytes = fCallbackName.getBytes(StandardCharsets.US_ASCII);
        int length = Math.min(bytes.length, this.fCallbackName.length);
        for (int i = 0; i < length; i++) {
            this.fCallbackName[i] = (char) bytes[i];
        }
        for (int i = length; i < this.fCallbackName.length; i++) {
            this.fCallbackName[i] = 0;
        }
    }

    /**
     * getfCallbackNameAsString
     *
     * @brief
     * 功能: 将回调名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 回调名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfCallbackNameAsString() {
        int endIndex = 0;
        while (endIndex < fCallbackName.length && fCallbackName[endIndex] != 0) {
            endIndex++;
        }
        byte[] bytes = new byte[endIndex];
        for (int i = 0; i < endIndex; i++) {
            bytes[i] = (byte) fCallbackName[i];
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * getfMaxQueueDepth
     *
     * @brief
     * 功能: 获取健康监控事件队列深度
     * 
     * @return
     * long: 健康监控事件队列深度
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxQueueDepth() {
        return fMaxQueueDepth;
    }

    /**
     * setfMaxQueueDepth
     *
     * @brief
     * 功能: 设置健康监控事件队列深度
     * 
     * @param[in]  fMaxQueueDepth: 健康监控事件队列深度
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxQueueDepth(long fMaxQueueDepth) {
        this.fMaxQueueDepth = fMaxQueueDepth;
    }

    /**
     * getfQueueThreshold
     *
     * @brief
     * 功能: 获取健康监控事件队列门限
     * 
     * @return
     * long: 健康监控事件队列门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfQueueThreshold() {
        return fQueueThreshold;
    }

    /**
     * setfQueueThreshold
     *
     * @brief
     * 功能: 设置健康监控事件队列门限
     * 
     * @param[in]  fQueueThreshold: 健康监控事件队列门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfQueueThreshold(long fQueueThreshold) {
        this.fQueueThreshold = fQueueThreshold;
    }

    /**
     * getfStackSize
     *
     * @brief
     * 功能: 获取健康监控任务栈大小
     * 
     * @return
     * long: 健康监控任务栈大小
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfStackSize() {
        return fStackSize;
    }

    /**
     * setfStackSize
     *
     * @brief
     * 功能: 设置健康监控任务栈大小
     * 
     * @param[in]  fStackSize: 健康监控任务栈大小
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfStackSize(long fStackSize) {
        this.fStackSize = fStackSize;
    }

    /**
     * getfMaxLogEntries
     *
     * @brief
     * 功能: 获取最大日志数量
     * 
     * @return
     * long: 最大日志数量
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxLogEntries() {
        return fMaxLogEntries;
    }

    /**
     * setfMaxLogEntries
     *
     * @brief
     * 功能: 设置最大日志数量
     * 
     * @param[in]  fMaxLogEntries: 最大日志数量
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxLogEntries(long fMaxLogEntries) {
        this.fMaxLogEntries = fMaxLogEntries;
    }

    /**
     * getfLogEntriesThreshold
     *
     * @brief
     * 功能: 获取日志数量门限
     * 
     * @return
     * long: 日志数量门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfLogEntriesThreshold() {
        return fLogEntriesThreshold;
    }

    /**
     * setfLogEntriesThreshold
     *
     * @brief
     * 功能: 设置日志数量门限
     * 
     * @param[in]  fLogEntriesThreshold: 日志数量门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfLogEntriesThreshold(long fLogEntriesThreshold) {
        this.fLogEntriesThreshold = fLogEntriesThreshold;
    }

    /**
     * getfAttributeMask
     *
     * @brief
     * 功能: 获取健康监控属性
     * 
     * @return
     * long: 健康监控属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfAttributeMask() {
        return fAttributeMask;
    }

    /**
     * setfAttributeMask
     *
     * @brief
     * 功能: 设置健康监控属性
     * 
     * @param[in]  fAttributeMask: 健康监控属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfAttributeMask(long fAttributeMask) {
        this.fAttributeMask = fAttributeMask;
    }

    /**
     * getfEventFilterMask
     *
     * @brief
     * 功能: 获取事件过滤属性
     * 
     * @return
     * long: 事件过滤属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfEventFilterMask() {
        return fEventFilterMask;
    }

    /**
     * setfEventFilterMask
     *
     * @brief
     * 功能: 设置事件过滤属性
     * 
     * @param[in]  fEventFilterMask: 事件过滤属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfEventFilterMask(long fEventFilterMask) {
        this.fEventFilterMask = fEventFilterMask;
    }

    /**
     * getfMaxErrorHandlerQueueDepth
     *
     * @brief
     * 功能: 获取核心级健康监控最大错误处理队列深度
     * 
     * @return
     * long: 核心级健康监控最大错误处理队列深度
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxErrorHandlerQueueDepth() {
        return fMaxErrorHandlerQueueDepth;
    }

    /**
     * setfMaxErrorHandlerQueueDepth
     *
     * @brief
     * 功能: 设置核心级健康监控最大错误处理队列深度
     * 
     * @param[in]  fMaxErrorHandlerQueueDepth: 核心级健康监控最大错误处理队列深度
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxErrorHandlerQueueDepth(long fMaxErrorHandlerQueueDepth) {
        this.fMaxErrorHandlerQueueDepth = fMaxErrorHandlerQueueDepth;
    }

    /**
     * getfCoreRefIds
     *
     * @brief
     * 功能: 获取配置所属的核心ID数组
     * 
     * @return
     * List<Long>: 核心ID数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<Long> getfCoreRefIds() {
        return fCoreRefIds;
    }

    /**
     * setfCoreRefIds
     *
     * @brief
     * 功能: 设置配置所属的核心ID数组
     * 
     * @param[in]  fCoreRefIds: 核心ID数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCoreRefIds(List<Long> fCoreRefIds) {
        this.fCoreRefIds = fCoreRefIds;
    }

    /**
     * addfCoreRefId
     *
     * @brief
     * 功能: 向核心ID数组添加一个核心ID
     * 
     * @param[in]  id: 核心ID
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addfCoreRefId(long id) {
        this.fCoreRefIds.add(id);
    }

    /**
     * getfCoreRefIdsCount
     *
     * @brief
     * 功能: 获取健康监控配置所属的核心ID个数
     * 
     * @return
     * long: 核心ID个数
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfCoreRefIdsCount() {
        return fCoreRefIdsCount;
    }

    /**
     * setfCoreRefIdsCount
     *
     * @brief
     * 功能: 设置健康监控配置所属的核心ID个数
     * 
     * @param[in]  fCoreRefIdsCount: 核心ID个数
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCoreRefIdsCount(long fCoreRefIdsCount) {
        this.fCoreRefIdsCount = fCoreRefIdsCount;
    }

    /**
     * getfErrorHandlerQueueThreshold
     *
     * @brief
     * 功能: 获取核心级健康监控错误处理事件队列门限
     * 
     * @return
     * long: 核心级健康监控错误处理事件队列门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfErrorHandlerQueueThreshold() {
        return fErrorHandlerQueueThreshold;
    }

    /**
     * setfErrorHandlerQueueThreshold
     *
     * @brief
     * 功能: 设置核心级健康监控错误处理事件队列门限
     * 
     * @param[in]  fErrorHandlerQueueThreshold: 核心级健康监控错误处理事件队列门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfErrorHandlerQueueThreshold(long fErrorHandlerQueueThreshold) {
        this.fErrorHandlerQueueThreshold = fErrorHandlerQueueThreshold;
    }

    /**
     * getName
     *
     * @brief
     * 功能: 获取任务名称，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * String: 任务名称
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getName() {
        return getfTaskNameAsString();
    }

    /**
     * setName
     *
     * @brief
     * 功能: 设置任务名称，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  name: 任务名称
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setName(String name) {
        setfTaskName(name);
    }

    /**
     * getCallback
     *
     * @brief
     * 功能: 获取回调名称，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * String: 回调名称
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getCallback() {
        return getfCallbackNameAsString();
    }

    /**
     * setCallback
     *
     * @brief
     * 功能: 设置回调名称，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  callback: 回调名称
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setCallback(String callback) {
        setfCallbackName(callback);
    }

    /**
     * getMaxQueueDepth
     *
     * @brief
     * 功能: 获取健康监控事件队列深度，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 健康监控事件队列深度
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getMaxQueueDepth() {
        return (int) fMaxQueueDepth;
    }

    /**
     * setMaxQueueDepth
     *
     * @brief
     * 功能: 设置健康监控事件队列深度，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  maxQueueDepth: 健康监控事件队列深度
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMaxQueueDepth(int maxQueueDepth) {
        this.fMaxQueueDepth = maxQueueDepth;
    }

    /**
     * getQueueThreshold
     *
     * @brief
     * 功能: 获取健康监控事件队列门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 健康监控事件队列门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getQueueThreshold() {
        return (int) fQueueThreshold;
    }

    /**
     * setQueueThreshold
     *
     * @brief
     * 功能: 设置健康监控事件队列门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  queueThreshold: 健康监控事件队列门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setQueueThreshold(int queueThreshold) {
        this.fQueueThreshold = queueThreshold;
    }

    /**
     * getStackSize
     *
     * @brief
     * 功能: 获取健康监控任务栈大小，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 健康监控任务栈大小
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getStackSize() {
        return (int) fStackSize;
    }

    /**
     * setStackSize
     *
     * @brief
     * 功能: 设置健康监控任务栈大小，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  stackSize: 健康监控任务栈大小
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setStackSize(int stackSize) {
        this.fStackSize = stackSize;
    }

    /**
     * getMaxLogEntries
     *
     * @brief
     * 功能: 获取最大日志数量，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 最大日志数量
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getMaxLogEntries() {
        return (int) fMaxLogEntries;
    }

    /**
     * setMaxLogEntries
     *
     * @brief
     * 功能: 设置最大日志数量，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  maxLogEntries: 最大日志数量
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMaxLogEntries(int maxLogEntries) {
        this.fMaxLogEntries = maxLogEntries;
    }

    /**
     * getLogEntriesThreshold
     *
     * @brief
     * 功能: 获取日志数量门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 日志数量门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getLogEntriesThreshold() {
        return (int) fLogEntriesThreshold;
    }

    /**
     * setLogEntriesThreshold
     *
     * @brief
     * 功能: 设置日志数量门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  logEntriesThreshold: 日志数量门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setLogEntriesThreshold(int logEntriesThreshold) {
        this.fLogEntriesThreshold = logEntriesThreshold;
    }

    /**
     * getAttributeMask
     *
     * @brief
     * 功能: 获取健康监控属性，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 健康监控属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getAttributeMask() {
        return (int) fAttributeMask;
    }

    /**
     * setAttributeMask
     *
     * @brief
     * 功能: 设置健康监控属性，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  attributeMask: 健康监控属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setAttributeMask(int attributeMask) {
        this.fAttributeMask = attributeMask;
    }

    /**
     * getEventFilterMask
     *
     * @brief
     * 功能: 获取事件过滤属性，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 事件过滤属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getEventFilterMask() {
        return (int) fEventFilterMask;
    }

    /**
     * setEventFilterMask
     *
     * @brief
     * 功能: 设置事件过滤属性，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  eventFilterMask: 事件过滤属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setEventFilterMask(int eventFilterMask) {
        this.fEventFilterMask = eventFilterMask;
    }

    /**
     * getMaxErrorHandlerQueueDepth
     *
     * @brief
     * 功能: 获取核心级健康监控最大错误处理队列深度，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 核心级健康监控最大错误处理队列深度
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getMaxErrorHandlerQueueDepth() {
        return (int) fMaxErrorHandlerQueueDepth;
    }

    /**
     * setMaxErrorHandlerQueueDepth
     *
     * @brief
     * 功能: 设置核心级健康监控最大错误处理队列深度，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  maxErrorHandlerQueueDepth: 核心级健康监控最大错误处理队列深度
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMaxErrorHandlerQueueDepth(int maxErrorHandlerQueueDepth) {
        this.fMaxErrorHandlerQueueDepth = maxErrorHandlerQueueDepth;
    }

    /**
     * getErrorHandlerQueueThreshold
     *
     * @brief
     * 功能: 获取核心级健康监控错误处理事件队列门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * int: 核心级健康监控错误处理事件队列门限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getErrorHandlerQueueThreshold() {
        return (int) fErrorHandlerQueueThreshold;
    }

    /**
     * setErrorHandlerQueueThreshold
     *
     * @brief
     * 功能: 设置核心级健康监控错误处理事件队列门限，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  errorHandlerQueueThreshold: 核心级健康监控错误处理事件队列门限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setErrorHandlerQueueThreshold(int errorHandlerQueueThreshold) {
        this.fErrorHandlerQueueThreshold = errorHandlerQueueThreshold;
    }

    /**
     * getCoreRefIds
     *
     * @brief
     * 功能: 获取配置所属的核心ID数组，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @return
     * List<Long>: 核心ID数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<Long> getCoreRefIds() {
        return fCoreRefIds;
    }

    /**
     * setCoreRefIds
     *
     * @brief
     * 功能: 设置配置所属的核心ID数组，为生成器提供的便捷方法，基于CoreHMTaskGenerator32big中的方法签名
     * 
     * @param[in]  coreRefIds: 核心ID数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setCoreRefIds(List<Long> coreRefIds) {
        this.fCoreRefIds = coreRefIds;
    }
}
