/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionHMTask.java
 * 
 * 描述    : 分区健康监控任务配置实体类
 *           对应XML中的 /Module/HealthMonitor/HMTask/PartitionHMTaskList/PartitionHMTask 节点
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

public class PartitionHMTask {

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
    private long fMaxErrorHandlerQueueDepth;    // 进程级健康监控最大错误处理队列深度, unsigned int, 4字节
    private List<Long> fPartitionRefIds = new ArrayList<>(); // 配置所属的核心或分区 ID 数组, unsigned int 数组, 255个, 每个4字节
    private long fPartitionRefIdsCount;         // 健康监控配置所属的核心或分区 ID 个数, unsigned int, 4字节
    private long fErrorHandlerQueueThreshold;   // 新增字段：进程级健康监控错误处理事件队列门限

    /**
     * PartitionHMTask
     *
     * @brief
     * 功能: 默认构造函数，初始化字符数组并将配置类型设为固定值
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public PartitionHMTask() {
        Arrays.fill(fTaskName, (char) 0);
        Arrays.fill(fCallbackName, (char) 0);
        // 默认值：配置类型固定值
        this.fConfigType = 1129468932L;
        // 接口文档规定stackSize未配置时默认取0x1800
        this.fStackSize = 0x1800L;
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
     * @param[in]  fTaskName: 任务名字符串
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
     * @param[in]  fCallbackName: 回调名称字符串
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
     * 功能: 获取进程级健康监控最大错误处理队列深度
     * 
     * @return
     * long: 进程级健康监控最大错误处理队列深度
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
     * 功能: 设置进程级健康监控最大错误处理队列深度
     * 
     * @param[in]  fMaxErrorHandlerQueueDepth: 进程级健康监控最大错误处理队列深度
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
     * getfPartitionRefIds
     *
     * @brief
     * 功能: 获取配置所属的核心或分区ID数组
     * 
     * @return
     * List<Long>: 核心或分区ID数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<Long> getfPartitionRefIds() {
        return fPartitionRefIds;
    }

    /**
     * setfPartitionRefIds
     *
     * @brief
     * 功能: 设置配置所属的核心或分区ID数组
     * 
     * @param[in]  fPartitionRefIds: 核心或分区ID数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfPartitionRefIds(List<Long> fPartitionRefIds) {
        this.fPartitionRefIds = fPartitionRefIds;
    }

    /**
     * addfPartitionRefId
     *
     * @brief
     * 功能: 向核心或分区ID数组添加一个ID
     * 
     * @param[in]  id: 核心或分区ID
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addfPartitionRefId(long id) {
        this.fPartitionRefIds.add(id);
    }

    /**
     * getfPartitionRefIdsCount
     *
     * @brief
     * 功能: 获取健康监控配置所属的核心或分区ID个数
     * 
     * @return
     * long: 核心或分区ID个数
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfPartitionRefIdsCount() {
        return fPartitionRefIdsCount;
    }

    /**
     * setfPartitionRefIdsCount
     *
     * @brief
     * 功能: 设置健康监控配置所属的核心或分区ID个数
     * 
     * @param[in]  fPartitionRefIdsCount: 核心或分区ID个数
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfPartitionRefIdsCount(long fPartitionRefIdsCount) {
        this.fPartitionRefIdsCount = fPartitionRefIdsCount;
    }

    /**
     * getfErrorHandlerQueueThreshold
     *
     * @brief
     * 功能: 获取进程级健康监控错误处理事件队列门限
     * 
     * @return
     * long: 进程级健康监控错误处理事件队列门限
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
     * 功能: 设置进程级健康监控错误处理事件队列门限
     * 
     * @param[in]  fErrorHandlerQueueThreshold: 进程级健康监控错误处理事件队列门限
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
}
