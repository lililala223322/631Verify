/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: HealthMonitor.java
 * 
 * 描述    : 健康监控配置实体类
 *           对应XML中的 /Module/HealthMonitor 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthMonitor {

    /* 成员变量 */
    /* /Module/HealthMonitor/HMTask/PartitionHMTaskList 下每有一个PartitionHMTask,表示
     * 多一个分区健康监控任务配置数据结构体。 */
    private Map<String, PartitionHMTask> fPartitionHMTasks;
    
    /* /Module/HealthMonitor/HMTask/CoreHMTaskList 下每有一个CoreHMTask,表示
     * 多一个核心健康监控任务配置数据结构体。 */
    private Map<String, CoreHMTask> fCoreHMTasks;
    
    /* 核心健康监控表 - 对应 <CoreHMTable> 节点 */
    private Map<String, CoreHMTable> coreHMTables;
    
    /* 多分区健康监控表 - 对应 <MultiPartitionHMTable> 节点 */
    private Map<String, MultiPartitionHMTable> multiPartitionHMTables;
    
    /* 分区健康监控表 - 对应 <PartitionHMTable> 节点 */
    private Map<String, PartitionHMTable> partitionHMTables;
    
    /* 核心健康监控状态 - 对应 <CoreHmState> 节点 */
    private Map<String, CoreHMState> coreHMStates;

    /**
     * HealthMonitor
     *
     * @brief
     * 功能: 默认构造函数，初始化各健康监控相关配置映射表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public HealthMonitor() {
        this.fPartitionHMTasks = new LinkedHashMap<>();
        this.fCoreHMTasks = new LinkedHashMap<>();
        this.coreHMTables = new LinkedHashMap<>();
        this.multiPartitionHMTables = new LinkedHashMap<>();
        this.partitionHMTables = new LinkedHashMap<>();
        this.coreHMStates = new LinkedHashMap<>();
    }

    /**
     * getfPartitionHMTasks
     *
     * @brief
     * 功能: 获取分区健康监控任务配置映射表
     * 
     * @return
     * Map<String, PartitionHMTask>: 分区健康监控任务配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, PartitionHMTask> getfPartitionHMTasks() {
        return fPartitionHMTasks;
    }

    /**
     * setfPartitionHMTasks
     *
     * @brief
     * 功能: 设置分区健康监控任务配置映射表
     * 
     * @param[in]  fPartitionHMTasks: 分区健康监控任务配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfPartitionHMTasks(Map<String, PartitionHMTask> fPartitionHMTasks) {
        this.fPartitionHMTasks = fPartitionHMTasks;
    }

    /**
     * addPartitionHMTask
     *
     * @brief
     * 功能: 向分区健康监控任务配置映射表添加一项
     * 
     * @param[in]  name: 任务名称
     * @param[in]  task: 分区健康监控任务配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartitionHMTask(String name, PartitionHMTask task) {
        this.fPartitionHMTasks.put(name, task);
    }

    /**
     * getfCoreHMTasks
     *
     * @brief
     * 功能: 获取核心健康监控任务配置映射表
     * 
     * @return
     * Map<String, CoreHMTask>: 核心健康监控任务配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, CoreHMTask> getfCoreHMTasks() {
        return fCoreHMTasks;
    }

    /**
     * setfCoreHMTasks
     *
     * @brief
     * 功能: 设置核心健康监控任务配置映射表
     * 
     * @param[in]  fCoreHMTasks: 核心健康监控任务配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCoreHMTasks(Map<String, CoreHMTask> fCoreHMTasks) {
        this.fCoreHMTasks = fCoreHMTasks;
    }

    /**
     * addCoreHMTask
     *
     * @brief
     * 功能: 向核心健康监控任务配置映射表添加一项
     * 
     * @param[in]  name: 任务名称
     * @param[in]  task: 核心健康监控任务配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addCoreHMTask(String name, CoreHMTask task) {
        this.fCoreHMTasks.put(name, task);
    }

    /**
     * getCoreHMTables
     *
     * @brief
     * 功能: 获取核心健康监控表配置映射表
     * 
     * @return
     * Map<String, CoreHMTable>: 核心健康监控表配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, CoreHMTable> getCoreHMTables() {
        return coreHMTables;
    }

    /**
     * setCoreHMTables
     *
     * @brief
     * 功能: 设置核心健康监控表配置映射表
     * 
     * @param[in]  coreHMTables: 核心健康监控表配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setCoreHMTables(Map<String, CoreHMTable> coreHMTables) {
        this.coreHMTables = coreHMTables;
    }

    /**
     * addCoreHMTable
     *
     * @brief
     * 功能: 向核心健康监控表配置映射表添加一项
     * 
     * @param[in]  name: 表名称
     * @param[in]  table: 核心健康监控表配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addCoreHMTable(String name, CoreHMTable table) {
        this.coreHMTables.put(name, table);
    }

    /**
     * getMultiPartitionHMTables
     *
     * @brief
     * 功能: 获取多分区健康监控表配置映射表
     * 
     * @return
     * Map<String, MultiPartitionHMTable>: 多分区健康监控表配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, MultiPartitionHMTable> getMultiPartitionHMTables() {
        return multiPartitionHMTables;
    }

    /**
     * setMultiPartitionHMTables
     *
     * @brief
     * 功能: 设置多分区健康监控表配置映射表
     * 
     * @param[in]  multiPartitionHMTables: 多分区健康监控表配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMultiPartitionHMTables(Map<String, MultiPartitionHMTable> multiPartitionHMTables) {
        this.multiPartitionHMTables = multiPartitionHMTables;
    }

    /**
     * addMultiPartitionHMTable
     *
     * @brief
     * 功能: 向多分区健康监控表配置映射表添加一项
     * 
     * @param[in]  name: 表名称
     * @param[in]  table: 多分区健康监控表配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addMultiPartitionHMTable(String name, MultiPartitionHMTable table) {
        this.multiPartitionHMTables.put(name, table);
    }

    /**
     * getPartitionHMTables
     *
     * @brief
     * 功能: 获取分区健康监控表配置映射表
     * 
     * @return
     * Map<String, PartitionHMTable>: 分区健康监控表配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, PartitionHMTable> getPartitionHMTables() {
        return partitionHMTables;
    }

    /**
     * setPartitionHMTables
     *
     * @brief
     * 功能: 设置分区健康监控表配置映射表
     * 
     * @param[in]  partitionHMTables: 分区健康监控表配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setPartitionHMTables(Map<String, PartitionHMTable> partitionHMTables) {
        this.partitionHMTables = partitionHMTables;
    }

    /**
     * addPartitionHMTable
     *
     * @brief
     * 功能: 向分区健康监控表配置映射表添加一项
     * 
     * @param[in]  name: 表名称
     * @param[in]  table: 分区健康监控表配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartitionHMTable(String name, PartitionHMTable table) {
        this.partitionHMTables.put(name, table);
    }

    /**
     * getCoreHMStates
     *
     * @brief
     * 功能: 获取核心健康监控状态配置映射表
     * 
     * @return
     * Map<String, CoreHMState>: 核心健康监控状态配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, CoreHMState> getCoreHMStates() {
        return coreHMStates;
    }

    /**
     * setCoreHMStates
     *
     * @brief
     * 功能: 设置核心健康监控状态配置映射表
     * 
     * @param[in]  coreHMStates: 核心健康监控状态配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setCoreHMStates(Map<String, CoreHMState> coreHMStates) {
        this.coreHMStates = coreHMStates;
    }

    /**
     * addCoreHMState
     *
     * @brief
     * 功能: 向核心健康监控状态配置映射表添加一项
     * 
     * @param[in]  key: 状态键
     * @param[in]  coreHMState: 核心健康监控状态配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addCoreHMState(String key, CoreHMState coreHMState) {
        this.coreHMStates.put(key, coreHMState);
    }
}