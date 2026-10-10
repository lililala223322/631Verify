/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Module.java
 * 
 * 描述    : 模块配置实体类
 *           对应XML中的 /Module 根节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.LinkedHashMap;
import java.util.Map;

public class Module {

    /* 成员变量 */
    private int fId;  // 模块的数字标识，取值范围0-255
    private String fCrc32;
    private CoreOS fCoreOS;
    private Map<String, ApplicationEntity> fApplications;
    private Map<String, SharedLibrary> fSharedLibraries;
    private Map<String, Partition> fPartitions;
    private Map<String, Schedule> fSchedules;
    private Map<Integer, Channel> fChannels; 
    private Map<String, LogBook> fLogBooks;
    private HealthMonitor fHealthMonitor;
    private Payload fPayload; // 新增Payload支持
    private Map<String, SharedData> fSharedDatas; // 新增SharedData集合支持
    private String fSharedDataRegionsSize; // SharedDataRegions元素的Size属性

    /**
     * Module
     *
     * @brief
     * 功能: 默认构造函数，初始化各配置映射表及子模块对象
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Module() {
        this.fApplications = new LinkedHashMap<>();
        this.fSharedLibraries = new LinkedHashMap<>();
        this.fPartitions = new LinkedHashMap<>();
        /* 初始化新增的日志记事本，使用LinkedHashMap保持插入顺序 */
        this.fLogBooks = new LinkedHashMap<>();
        /* 初始化新增的健康监控模块 */
        this.fHealthMonitor = new HealthMonitor();
        this.fSchedules = new LinkedHashMap<>();
        this.fChannels = new LinkedHashMap<>();
        /* 初始化Payload */
        this.fPayload = new Payload();
        /* 初始化SharedData集合 */
        this.fSharedDatas = new LinkedHashMap<>();
    }
    
    /**
     * getfId
     *
     * @brief
     * 功能: 获取模块的数字标识
     * 
     * @return
     * int: 模块的数字标识，取值范围0-255
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getfId() {
        return fId;
    }
    
    /**
     * setfId
     *
     * @brief
     * 功能: 设置模块的数字标识
     * 
     * @param[in]  fId: 模块的数字标识，取值范围0-255
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfId(int fId) {
        this.fId = fId;
    }
    
    /**
     * getfCrc32
     *
     * @brief
     * 功能: 获取模块CRC32校验值
     * 
     * @return
     * String: 模块CRC32校验值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfCrc32() {
        return fCrc32;
    }
    
    /**
     * setfCrc32
     *
     * @brief
     * 功能: 设置模块CRC32校验值
     * 
     * @param[in]  fCrc32: 模块CRC32校验值
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCrc32(String fCrc32) {
        this.fCrc32 = fCrc32;
    }
    
    /**
     * getfCoreOS
     *
     * @brief
     * 功能: 获取核心操作系统配置对象
     * 
     * @return
     * CoreOS: 核心操作系统配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreOS getfCoreOS() {
        return fCoreOS;
    }
    
    /**
     * setfCoreOS
     *
     * @brief
     * 功能: 设置核心操作系统配置对象
     * 
     * @param[in]  fCoreOS: 核心操作系统配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCoreOS(CoreOS fCoreOS) {
        this.fCoreOS = fCoreOS;
    }
    
    /**
     * getfApplications
     *
     * @brief
     * 功能: 获取应用配置映射表
     * 
     * @return
     * Map<String, ApplicationEntity>: 应用配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, ApplicationEntity> getfApplications() {
        return fApplications;
    }
    
    /**
     * setfApplications
     *
     * @brief
     * 功能: 设置应用配置映射表
     * 
     * @param[in]  fApplications: 应用配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfApplications(Map<String, ApplicationEntity> fApplications) {
        this.fApplications = fApplications;
    }
    
    /**
     * addApplication
     *
     * @brief
     * 功能: 向应用配置映射表添加一项
     * 
     * @param[in]  name: 应用名称
     * @param[in]  application: 应用配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addApplication(String name, ApplicationEntity application) {
        this.fApplications.put(name, application);
    }
    
    /**
     * getfSharedLibraries
     *
     * @brief
     * 功能: 获取共享库配置映射表
     * 
     * @return
     * Map<String, SharedLibrary>: 共享库配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, SharedLibrary> getfSharedLibraries() {
        return fSharedLibraries;
    }
    
    /**
     * setfSharedLibraries
     *
     * @brief
     * 功能: 设置共享库配置映射表
     * 
     * @param[in]  fSharedLibraries: 共享库配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedLibraries(Map<String, SharedLibrary> fSharedLibraries) {
        this.fSharedLibraries = fSharedLibraries;
    }
    
    /**
     * addSharedLibrary
     *
     * @brief
     * 功能: 向共享库配置映射表添加一项
     * 
     * @param[in]  name: 共享库名称
     * @param[in]  sharedLibrary: 共享库配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSharedLibrary(String name, SharedLibrary sharedLibrary) {
        this.fSharedLibraries.put(name, sharedLibrary);
    }
    
    /**
     * getfPartitions
     *
     * @brief
     * 功能: 获取分区配置映射表
     * 
     * @return
     * Map<String, Partition>: 分区配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, Partition> getfPartitions() {
        return fPartitions;
    }
    
    /**
     * setfPartitions
     *
     * @brief
     * 功能: 设置分区配置映射表
     * 
     * @param[in]  fPartitions: 分区配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfPartitions(Map<String, Partition> fPartitions) {
        this.fPartitions = fPartitions;
    }

    /**
     * addPartition
     *
     * @brief
     * 功能: 向分区配置映射表添加一项
     * 
     * @param[in]  name: 分区名称
     * @param[in]  partition: 分区配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartition(String name, Partition partition) {
        this.fPartitions.put(name, partition);
    }

    /**
     * getfLogBooks
     *
     * @brief
     * 功能: 获取日志记事本配置映射表
     * 
     * @return
     * Map<String, LogBook>: 日志记事本配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, LogBook> getfLogBooks() {
        return fLogBooks;
    }

    /**
     * setfLogBooks
     *
     * @brief
     * 功能: 设置日志记事本配置映射表
     * 
     * @param[in]  fLogBooks: 日志记事本配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfLogBooks(Map<String, LogBook> fLogBooks) {
        this.fLogBooks = fLogBooks;
    }

    /**
     * addLogBook
     *
     * @brief
     * 功能: 向日志记事本配置映射表添加一项
     * 
     * @param[in]  name: 日志记事本名称
     * @param[in]  logBook: 日志记事本配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addLogBook(String name, LogBook logBook) {
        this.fLogBooks.put(name, logBook);
    }

    /**
     * getfHealthMonitor
     *
     * @brief
     * 功能: 获取健康监控配置对象
     * 
     * @return
     * HealthMonitor: 健康监控配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public HealthMonitor getfHealthMonitor() {
        return fHealthMonitor;
    }

    /**
     * setfHealthMonitor
     *
     * @brief
     * 功能: 设置健康监控配置对象
     * 
     * @param[in]  fHealthMonitor: 健康监控配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfHealthMonitor(HealthMonitor fHealthMonitor) {
        this.fHealthMonitor = fHealthMonitor;
    }
    
    /**
     * getfSchedules
     *
     * @brief
     * 功能: 获取调度配置映射表
     * 
     * @return
     * Map<String, Schedule>: 调度配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, Schedule> getfSchedules() {
        return fSchedules;
    }
    
    /**
     * setfSchedules
     *
     * @brief
     * 功能: 设置调度配置映射表
     * 
     * @param[in]  fSchedules: 调度配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSchedules(Map<String, Schedule> fSchedules) {
        this.fSchedules = fSchedules;
    }
    
    /**
     * addSchedule
     *
     * @brief
     * 功能: 向调度配置映射表添加一项，若schedule为null则输出警告并忽略
     * 
     * @param[in]  name: 调度名称
     * @param[in]  schedule: 调度配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSchedule(String name, Schedule schedule) {
        if (schedule != null) {
            this.fSchedules.put(name, schedule);
        } else {
            System.err.println("Warning: attempt to add null Schedule, ignored. name=" + name);
        }
    }

    /**
     * getfChannels
     *
     * @brief
     * 功能: 获取通道配置映射表
     * 
     * @return
     * Map<Integer, Channel>: 通道配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<Integer, Channel> getfChannels() {
        return fChannels;
    }
    
    /**
     * setfChannels
     *
     * @brief
     * 功能: 设置通道配置映射表
     * 
     * @param[in]  fChannels: 通道配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfChannels(Map<Integer, Channel> fChannels) {
        this.fChannels = fChannels;
    }
    
    /**
     * addChannel
     *
     * @brief
     * 功能: 向通道配置映射表添加一项，若channel为null则输出警告并忽略
     * 
     * @param[in]  channelId: 通道ID
     * @param[in]  channel: 通道配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addChannel(int channelId, Channel channel) {
        if (channel != null) {
            this.fChannels.put(channelId, channel);
        } else {
            System.err.println("Warning: attempt to add null Channel, ignored. channelId=" + channelId);
        }
    }

    /**
     * getfPayload
     *
     * @brief
     * 功能: 获取Payload配置对象
     * 
     * @return
     * Payload: Payload配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Payload getfPayload() {
        return fPayload;
    }

    /**
     * setfPayload
     *
     * @brief
     * 功能: 设置Payload配置对象
     * 
     * @param[in]  fPayload: Payload配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfPayload(Payload fPayload) {
        this.fPayload = fPayload;
    }

    /**
     * getfSharedDatas
     *
     * @brief
     * 功能: 获取共享数据配置映射表
     * 
     * @return
     * Map<String, SharedData>: 共享数据配置映射表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<String, SharedData> getfSharedDatas() {
        return fSharedDatas;
    }

    /**
     * setfSharedDatas
     *
     * @brief
     * 功能: 设置共享数据配置映射表
     * 
     * @param[in]  fSharedDatas: 共享数据配置映射表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedDatas(Map<String, SharedData> fSharedDatas) {
        this.fSharedDatas = fSharedDatas;
    }

    /**
     * addSharedData
     *
     * @brief
     * 功能: 向共享数据配置映射表添加一项
     * 
     * @param[in]  name: 共享数据名称
     * @param[in]  sharedData: 共享数据配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSharedData(String name, SharedData sharedData) {
        this.fSharedDatas.put(name, sharedData);
    }

    /**
     * getfSharedDataRegionsSize
     *
     * @brief
     * 功能: 获取SharedDataRegions元素的Size属性
     * 
     * @return
     * String: SharedDataRegions元素的Size属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfSharedDataRegionsSize() {
        return fSharedDataRegionsSize;
    }

    /**
     * setfSharedDataRegionsSize
     *
     * @brief
     * 功能: 设置SharedDataRegions元素的Size属性
     * 
     * @param[in]  fSharedDataRegionsSize: SharedDataRegions元素的Size属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedDataRegionsSize(String fSharedDataRegionsSize) {
        this.fSharedDataRegionsSize = fSharedDataRegionsSize;
    }
}
