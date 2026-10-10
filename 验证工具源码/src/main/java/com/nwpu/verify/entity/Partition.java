/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Partition.java
 * 
 * 描述    : 分区配置实体类
 *           对应XML中的 /Module/Partitions/Partition 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Partition {

    /* 成员变量 */
    private int fIdentifier;
    private int fBoundProcessorCore;
    private char[] fName = new char[32];
    private String fApplicationNameRef;
    private List<String> fSharedLibraryNameRefs;
    private Settings fSettings;
    /* 端口支持 */
    private List<QueuingPort> fQueuingPorts;
    private List<SamplingPort> fSamplingPorts;
    /* 共享数据区支持 */
    private List<SharedDataRegion> fSharedDataRegions;
    /* 伪分区标志 */
    private boolean fIsPseudoPartition;
    /* 日志记事本支持 */
    private List<LogBook> fLogBooks;

    /**
     * getfLogBooks
     *
     * @brief
     * 功能: 获取日志记事本列表
     * 
     * @return
     * List<LogBook>: 日志记事本列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<LogBook> getfLogBooks() {
        return fLogBooks;
    }

    /**
     * setfLogBooks
     *
     * @brief
     * 功能: 设置日志记事本列表
     * 
     * @param[in]  fLogBooks: 日志记事本列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfLogBooks(List<LogBook> fLogBooks) {
        this.fLogBooks = fLogBooks;
    }

    /**
     * addLogBook
     *
     * @brief
     * 功能: 向日志记事本列表添加一项
     * 
     * @param[in]  logBook: 日志记事本配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addLogBook(LogBook logBook) {
        if (this.fLogBooks == null) {
            this.fLogBooks = new ArrayList<>();
        }
        this.fLogBooks.add(logBook);
    }

    /**
     * Partition
     *
     * @brief
     * 功能: 默认构造函数，初始化共享库引用、设置、端口、共享数据区及日志记事本列表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Partition() {
        this.fSharedLibraryNameRefs = new ArrayList<>();
        this.fSettings = new Settings();
        /* 初始化端口列表 */
        this.fQueuingPorts = new ArrayList<>();
        this.fSamplingPorts = new ArrayList<>();
        /* 初始化共享数据区列表 */
        this.fSharedDataRegions = new ArrayList<>();
        this.fIsPseudoPartition = false;
        this.fLogBooks = new ArrayList<>();
    }

    /**
     * getfIdentifier
     *
     * @brief
     * 功能: 获取分区标识符
     * 
     * @return
     * int: 分区标识符
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getfIdentifier() {
        return fIdentifier;
    }

    /**
     * setfIdentifier
     *
     * @brief
     * 功能: 设置分区标识符
     * 
     * @param[in]  fIdentifier: 分区标识符
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfIdentifier(int fIdentifier) {
        this.fIdentifier = fIdentifier;
    }

    /**
     * getfBoundProcessorCore
     *
     * @brief
     * 功能: 获取绑定处理器核心编号
     * 
     * @return
     * int: 绑定处理器核心编号
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getfBoundProcessorCore() {
        return fBoundProcessorCore;
    }

    /**
     * setfBoundProcessorCore
     *
     * @brief
     * 功能: 设置绑定处理器核心编号
     * 
     * @param[in]  fBoundProcessorCore: 绑定处理器核心编号
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfBoundProcessorCore(int fBoundProcessorCore) {
        this.fBoundProcessorCore = fBoundProcessorCore;
    }

    /**
     * getfName
     *
     * @brief
     * 功能: 获取分区名称字符数组
     * 
     * @return
     * char[]: 分区名称字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getfName() {
        return fName;
    }

    /**
     * setfName
     *
     * @brief
     * 功能: 以字符数组设置分区名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: 分区名称字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfName(char[] fName) {
        int length = Math.min(fName.length, this.fName.length);
        for (int i = 0; i < length; i++) {
            this.fName[i] = fName[i];
        }
        for (int i = length; i < this.fName.length; i++) {
            this.fName[i] = 0;
        }
    }

    /**
     * setfName
     *
     * @brief
     * 功能: 以字符串设置分区名称
     * 
     * @param[in]  fName: 分区名称字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfName(String fName) {
        setfName(fName.toCharArray());
    }

    /**
     * getfNameAsString
     *
     * @brief
     * 功能: 将分区名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 分区名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfNameAsString() {
        int endIndex = 0;
        while (endIndex < fName.length && fName[endIndex] != 0) {
            endIndex++;
        }
        return new String(fName, 0, endIndex);
    }

    /**
     * getfApplicationNameRef
     *
     * @brief
     * 功能: 获取应用名称引用
     * 
     * @return
     * String: 应用名称引用
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfApplicationNameRef() {
        return fApplicationNameRef;
    }

    /**
     * setfApplicationNameRef
     *
     * @brief
     * 功能: 设置应用名称引用
     * 
     * @param[in]  fApplicationNameRef: 应用名称引用
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfApplicationNameRef(String fApplicationNameRef) {
        this.fApplicationNameRef = fApplicationNameRef;
    }

    /**
     * getfSharedLibraryNameRefs
     *
     * @brief
     * 功能: 获取共享库名称引用列表
     * 
     * @return
     * List<String>: 共享库名称引用列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<String> getfSharedLibraryNameRefs() {
        return fSharedLibraryNameRefs;
    }

    /**
     * setfSharedLibraryNameRefs
     *
     * @brief
     * 功能: 设置共享库名称引用列表
     * 
     * @param[in]  fSharedLibraryNameRefs: 共享库名称引用列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedLibraryNameRefs(List<String> fSharedLibraryNameRefs) {
        this.fSharedLibraryNameRefs = fSharedLibraryNameRefs;
    }

    /**
     * addSharedLibraryNameRef
     *
     * @brief
     * 功能: 向共享库名称引用列表添加一项
     * 
     * @param[in]  sharedLibraryNameRef: 共享库名称引用
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSharedLibraryNameRef(String sharedLibraryNameRef) {
        this.fSharedLibraryNameRefs.add(sharedLibraryNameRef);
    }

    /**
     * getfSettings
     *
     * @brief
     * 功能: 获取分区设置对象
     * 
     * @return
     * Settings: 分区设置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Settings getfSettings() {
        return fSettings;
    }

    /**
     * setfSettings
     *
     * @brief
     * 功能: 设置分区设置对象
     * 
     * @param[in]  fSettings: 分区设置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSettings(Settings fSettings) {
        this.fSettings = fSettings;
    }

    /**
     * getfQueuingPorts
     *
     * @brief
     * 功能: 获取排队端口列表
     * 
     * @return
     * List<QueuingPort>: 排队端口列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<QueuingPort> getfQueuingPorts() {
        return fQueuingPorts;
    }

    /**
     * setfQueuingPorts
     *
     * @brief
     * 功能: 设置排队端口列表
     * 
     * @param[in]  fQueuingPorts: 排队端口列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfQueuingPorts(List<QueuingPort> fQueuingPorts) {
        this.fQueuingPorts = fQueuingPorts;
    }

    /**
     * addQueuingPort
     *
     * @brief
     * 功能: 向排队端口列表添加一项
     * 
     * @param[in]  queuingPort: 排队端口配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addQueuingPort(QueuingPort queuingPort) {
        this.fQueuingPorts.add(queuingPort);
    }

    /**
     * getfSamplingPorts
     *
     * @brief
     * 功能: 获取采样端口列表
     * 
     * @return
     * List<SamplingPort>: 采样端口列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<SamplingPort> getfSamplingPorts() {
        return fSamplingPorts;
    }

    /**
     * setfSamplingPorts
     *
     * @brief
     * 功能: 设置采样端口列表
     * 
     * @param[in]  fSamplingPorts: 采样端口列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSamplingPorts(List<SamplingPort> fSamplingPorts) {
        this.fSamplingPorts = fSamplingPorts;
    }

    /**
     * addSamplingPort
     *
     * @brief
     * 功能: 向采样端口列表添加一项
     * 
     * @param[in]  samplingPort: 采样端口配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSamplingPort(SamplingPort samplingPort) {
        this.fSamplingPorts.add(samplingPort);
    }

    /**
     * getfSharedDataRegions
     *
     * @brief
     * 功能: 获取共享数据区列表
     * 
     * @return
     * List<SharedDataRegion>: 共享数据区列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<SharedDataRegion> getfSharedDataRegions() {
        return fSharedDataRegions;
    }

    /**
     * setfSharedDataRegions
     *
     * @brief
     * 功能: 设置共享数据区列表
     * 
     * @param[in]  fSharedDataRegions: 共享数据区列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedDataRegions(List<SharedDataRegion> fSharedDataRegions) {
        this.fSharedDataRegions = fSharedDataRegions;
    }

    /**
     * addSharedDataRegion
     *
     * @brief
     * 功能: 向共享数据区列表添加一项
     * 
     * @param[in]  sharedDataRegion: 共享数据区配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSharedDataRegion(SharedDataRegion sharedDataRegion) {
        this.fSharedDataRegions.add(sharedDataRegion);
    }

    /**
     * isFIsPseudoPartition
     *
     * @brief
     * 功能: 获取是否为伪分区标志
     * 
     * @return
     * boolean: 是否为伪分区
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public boolean isFIsPseudoPartition() {
        return fIsPseudoPartition;
    }

    /**
     * setfIsPseudoPartition
     *
     * @brief
     * 功能: 设置是否为伪分区标志
     * 
     * @param[in]  fIsPseudoPartition: 是否为伪分区
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfIsPseudoPartition(boolean fIsPseudoPartition) {
        this.fIsPseudoPartition = fIsPseudoPartition;
    }

    /**
     * getAllPorts
     *
     * @brief
     * 功能: 获取分区的所有端口（QueuingPort + SamplingPort），按照添加顺序返回
     * 
     * @return
     * List<Port>: 分区的所有端口列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(fQueuingPorts);
        allPorts.addAll(fSamplingPorts);
        return allPorts;
    }

    /**
     * Settings
     *
     * @brief
     * 功能: 内部类：Settings，分区设置配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class Settings {
        private long fRequiredMemorySize;
        private int fMaxGlobalFDs;
        private boolean fSystemPartition;
        private long fWatchDogDuration;
        private long fPeriod;
        private long fDuration;
        private int fNumWorkerTasks;
        private long fSyscallPermissions;
        private int fNumFiles;
        private int fNumDrivers;
        private int fNumLogMsgs;
        private int fNumStackGuardPages;
        private long fInitProcStackSize;
        private long fIsrStackSize;
        private long fSelSvrQSize;
        private boolean fAllocDisable;
        private long fMaxEventQStallDuration;
        private boolean fFpExcEnable;
        private boolean fAppsIdleRelinquishEnabled;
        private int fAppsPriority;
        private boolean fSetModuleSchedule;
        private String fScheduleChangeAction;
        private long fObjResSpaceSize;
        private long fTaskExpStackSize;
        private long fIdleTaskStackSize;
        private long fInitTaskStackSize;
        private int fInitTaskPriority;
        private char[] fDefaultAppName = new char[32];
        private int fUserExtension1;
        private int fUserExtension2;

        /**
         * Settings
         *
         * @brief
         * 功能: 默认构造函数，初始化分区设置默认值
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public Settings() {
            this.fWatchDogDuration = 1L;
            this.fPeriod = 2L;
            this.fDuration = 3L;
            this.fMaxGlobalFDs = 20;
            this.fNumWorkerTasks = 0;
            this.fSyscallPermissions = 0xFFFFFFFFL;
            this.fNumFiles = 0;
            this.fNumDrivers = 0;
            this.fNumLogMsgs = 0;
            this.fNumStackGuardPages = 0;
            this.fInitProcStackSize = 0x4000L;
            this.fIsrStackSize = 0x2000L;
            this.fSelSvrQSize = 0L;
            this.fAllocDisable = true;
            this.fMaxEventQStallDuration = -1L;
            this.fFpExcEnable = true;
            this.fAppsIdleRelinquishEnabled = false;
            this.fAppsPriority = -1;
            this.fSystemPartition = false;
            this.fSetModuleSchedule = false;
            this.fScheduleChangeAction = "IGNORE";
            this.fObjResSpaceSize = 0L;
            this.fTaskExpStackSize = 0x2000L;
            this.fIdleTaskStackSize = 0x2000L;
            this.fInitTaskStackSize = 0x4000L;
            this.fInitTaskPriority = 10;
            Arrays.fill(this.fDefaultAppName, (char) 0);
            this.fUserExtension1 = 0;
            this.fUserExtension2 = 0;
        }

        /**
         * getfRequiredMemorySize
         *
         * @brief
         * 功能: 获取所需内存大小
         * 
         * @return
         * long: 所需内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfRequiredMemorySize() {
            return fRequiredMemorySize;
        }

        /**
         * setfRequiredMemorySize
         *
         * @brief
         * 功能: 设置所需内存大小
         * 
         * @param[in]  fRequiredMemorySize: 所需内存大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfRequiredMemorySize(long fRequiredMemorySize) {
            this.fRequiredMemorySize = fRequiredMemorySize;
        }

        /**
         * getfMaxGlobalFDs
         *
         * @brief
         * 功能: 获取最大全局文件描述符数量
         * 
         * @return
         * int: 最大全局文件描述符数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfMaxGlobalFDs() {
            return fMaxGlobalFDs;
        }

        /**
         * setfMaxGlobalFDs
         *
         * @brief
         * 功能: 设置最大全局文件描述符数量
         * 
         * @param[in]  fMaxGlobalFDs: 最大全局文件描述符数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMaxGlobalFDs(int fMaxGlobalFDs) {
            this.fMaxGlobalFDs = fMaxGlobalFDs;
        }

        /**
         * getfSystemPartition
         *
         * @brief
         * 功能: 获取是否为系统分区
         * 
         * @return
         * boolean: 是否为系统分区
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean getfSystemPartition() {
            return fSystemPartition;
        }

        /**
         * setfSystemPartition
         *
         * @brief
         * 功能: 设置是否为系统分区
         * 
         * @param[in]  fSystemPartition: 是否为系统分区
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSystemPartition(boolean fSystemPartition) {
            this.fSystemPartition = fSystemPartition;
        }

        /**
         * getfWatchDogDuration
         *
         * @brief
         * 功能: 获取看门狗持续时间
         * 
         * @return
         * long: 看门狗持续时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfWatchDogDuration() {
            return fWatchDogDuration;
        }

        /**
         * setfWatchDogDuration
         *
         * @brief
         * 功能: 设置看门狗持续时间
         * 
         * @param[in]  fWatchDogDuration: 看门狗持续时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfWatchDogDuration(long fWatchDogDuration) {
            this.fWatchDogDuration = fWatchDogDuration;
        }

        /**
         * getfPeriod
         *
         * @brief
         * 功能: 获取周期
         * 
         * @return
         * long: 周期
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfPeriod() {
            return fPeriod;
        }

        /**
         * setfPeriod
         *
         * @brief
         * 功能: 设置周期
         * 
         * @param[in]  fPeriod: 周期
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPeriod(long fPeriod) {
            this.fPeriod = fPeriod;
        }

        /**
         * getfDuration
         *
         * @brief
         * 功能: 获取持续时间
         * 
         * @return
         * long: 持续时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfDuration() {
            return fDuration;
        }

        /**
         * setfDuration
         *
         * @brief
         * 功能: 设置持续时间
         * 
         * @param[in]  fDuration: 持续时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfDuration(long fDuration) {
            this.fDuration = fDuration;
        }

        /**
         * getfNumWorkerTasks
         *
         * @brief
         * 功能: 获取工作任务数量
         * 
         * @return
         * int: 工作任务数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfNumWorkerTasks() {
            return fNumWorkerTasks;
        }

        /**
         * setfNumWorkerTasks
         *
         * @brief
         * 功能: 设置工作任务数量
         * 
         * @param[in]  fNumWorkerTasks: 工作任务数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNumWorkerTasks(int fNumWorkerTasks) {
            this.fNumWorkerTasks = fNumWorkerTasks;
        }

        /**
         * getfSyscallPermissions
         *
         * @brief
         * 功能: 获取系统调用权限
         * 
         * @return
         * long: 系统调用权限
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfSyscallPermissions() {
            return fSyscallPermissions;
        }

        /**
         * setfSyscallPermissions
         *
         * @brief
         * 功能: 设置系统调用权限
         * 
         * @param[in]  fSyscallPermissions: 系统调用权限
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSyscallPermissions(long fSyscallPermissions) {
            this.fSyscallPermissions = fSyscallPermissions;
        }

        /**
         * getfNumFiles
         *
         * @brief
         * 功能: 获取文件数量
         * 
         * @return
         * int: 文件数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfNumFiles() {
            return fNumFiles;
        }

        /**
         * setfNumFiles
         *
         * @brief
         * 功能: 设置文件数量
         * 
         * @param[in]  fNumFiles: 文件数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNumFiles(int fNumFiles) {
            this.fNumFiles = fNumFiles;
        }

        /**
         * getfNumDrivers
         *
         * @brief
         * 功能: 获取驱动数量
         * 
         * @return
         * int: 驱动数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfNumDrivers() {
            return fNumDrivers;
        }

        /**
         * setfNumDrivers
         *
         * @brief
         * 功能: 设置驱动数量
         * 
         * @param[in]  fNumDrivers: 驱动数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNumDrivers(int fNumDrivers) {
            this.fNumDrivers = fNumDrivers;
        }

        /**
         * getfNumLogMsgs
         *
         * @brief
         * 功能: 获取日志消息数量
         * 
         * @return
         * int: 日志消息数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfNumLogMsgs() {
            return fNumLogMsgs;
        }

        /**
         * setfNumLogMsgs
         *
         * @brief
         * 功能: 设置日志消息数量
         * 
         * @param[in]  fNumLogMsgs: 日志消息数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNumLogMsgs(int fNumLogMsgs) {
            this.fNumLogMsgs = fNumLogMsgs;
        }

        /**
         * getfNumStackGuardPages
         *
         * @brief
         * 功能: 获取栈保护页数量
         * 
         * @return
         * int: 栈保护页数量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfNumStackGuardPages() {
            return fNumStackGuardPages;
        }

        /**
         * setfNumStackGuardPages
         *
         * @brief
         * 功能: 设置栈保护页数量
         * 
         * @param[in]  fNumStackGuardPages: 栈保护页数量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNumStackGuardPages(int fNumStackGuardPages) {
            this.fNumStackGuardPages = fNumStackGuardPages;
        }

        /**
         * getfInitProcStackSize
         *
         * @brief
         * 功能: 获取初始化进程栈大小
         * 
         * @return
         * long: 初始化进程栈大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfInitProcStackSize() {
            return fInitProcStackSize;
        }

        /**
         * setfInitProcStackSize
         *
         * @brief
         * 功能: 设置初始化进程栈大小
         * 
         * @param[in]  fInitProcStackSize: 初始化进程栈大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfInitProcStackSize(long fInitProcStackSize) {
            this.fInitProcStackSize = fInitProcStackSize;
        }

        /**
         * getfIsrStackSize
         *
         * @brief
         * 功能: 获取ISR栈大小
         * 
         * @return
         * long: ISR栈大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfIsrStackSize() {
            return fIsrStackSize;
        }

        /**
         * setfIsrStackSize
         *
         * @brief
         * 功能: 设置ISR栈大小
         * 
         * @param[in]  fIsrStackSize: ISR栈大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfIsrStackSize(long fIsrStackSize) {
            this.fIsrStackSize = fIsrStackSize;
        }

        /**
         * getfSelSvrQSize
         *
         * @brief
         * 功能: 获取选择服务器队列大小
         * 
         * @return
         * long: 选择服务器队列大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfSelSvrQSize() {
            return fSelSvrQSize;
        }

        /**
         * setfSelSvrQSize
         *
         * @brief
         * 功能: 设置选择服务器队列大小
         * 
         * @param[in]  fSelSvrQSize: 选择服务器队列大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSelSvrQSize(long fSelSvrQSize) {
            this.fSelSvrQSize = fSelSvrQSize;
        }

        /**
         * isfAllocDisable
         *
         * @brief
         * 功能: 获取内存分配禁用标志
         * 
         * @return
         * boolean: 内存分配是否禁用
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isfAllocDisable() {
            return fAllocDisable;
        }

        /**
         * setfAllocDisable
         *
         * @brief
         * 功能: 设置内存分配禁用标志
         * 
         * @param[in]  fAllocDisable: 内存分配是否禁用
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAllocDisable(boolean fAllocDisable) {
            this.fAllocDisable = fAllocDisable;
        }

        /**
         * getfMaxEventQStallDuration
         *
         * @brief
         * 功能: 获取最大事件队列停滞时间
         * 
         * @return
         * long: 最大事件队列停滞时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMaxEventQStallDuration() {
            return fMaxEventQStallDuration;
        }

        /**
         * setfMaxEventQStallDuration
         *
         * @brief
         * 功能: 设置最大事件队列停滞时间
         * 
         * @param[in]  fMaxEventQStallDuration: 最大事件队列停滞时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMaxEventQStallDuration(long fMaxEventQStallDuration) {
            this.fMaxEventQStallDuration = fMaxEventQStallDuration;
        }

        /**
         * isfFpExcEnable
         *
         * @brief
         * 功能: 获取浮点异常使能标志
         * 
         * @return
         * boolean: 浮点异常是否使能
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isfFpExcEnable() {
            return fFpExcEnable;
        }

        /**
         * setfFpExcEnable
         *
         * @brief
         * 功能: 设置浮点异常使能标志
         * 
         * @param[in]  fFpExcEnable: 浮点异常是否使能
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfFpExcEnable(boolean fFpExcEnable) {
            this.fFpExcEnable = fFpExcEnable;
        }

        /**
         * isfAppsIdleRelinquishEnabled
         *
         * @brief
         * 功能: 获取应用空闲让出使能标志
         * 
         * @return
         * boolean: 应用空闲让出是否使能
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isfAppsIdleRelinquishEnabled() {
            return fAppsIdleRelinquishEnabled;
        }

        /**
         * setfAppsIdleRelinquishEnabled
         *
         * @brief
         * 功能: 设置应用空闲让出使能标志
         * 
         * @param[in]  fAppsIdleRelinquishEnabled: 应用空闲让出是否使能
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAppsIdleRelinquishEnabled(boolean fAppsIdleRelinquishEnabled) {
            this.fAppsIdleRelinquishEnabled = fAppsIdleRelinquishEnabled;
        }

        /**
         * getfAppsPriority
         *
         * @brief
         * 功能: 获取应用优先级
         * 
         * @return
         * int: 应用优先级
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfAppsPriority() {
            return fAppsPriority;
        }

        /**
         * setfAppsPriority
         *
         * @brief
         * 功能: 设置应用优先级
         * 
         * @param[in]  fAppsPriority: 应用优先级
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAppsPriority(int fAppsPriority) {
            this.fAppsPriority = fAppsPriority;
        }

        /**
         * isfSetModuleSchedule
         *
         * @brief
         * 功能: 获取设置模块调度标志
         * 
         * @return
         * boolean: 是否设置模块调度
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isfSetModuleSchedule() {
            return fSetModuleSchedule;
        }

        /**
         * setfSetModuleSchedule
         *
         * @brief
         * 功能: 设置模块调度标志
         * 
         * @param[in]  fSetModuleSchedule: 是否设置模块调度
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSetModuleSchedule(boolean fSetModuleSchedule) {
            this.fSetModuleSchedule = fSetModuleSchedule;
        }

        /**
         * getfScheduleChangeAction
         *
         * @brief
         * 功能: 获取调度切换动作
         * 
         * @return
         * String: 调度切换动作
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfScheduleChangeAction() {
            return fScheduleChangeAction;
        }

        /**
         * setfScheduleChangeAction
         *
         * @brief
         * 功能: 设置调度切换动作
         * 
         * @param[in]  fScheduleChangeAction: 调度切换动作
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfScheduleChangeAction(String fScheduleChangeAction) {
            this.fScheduleChangeAction = fScheduleChangeAction;
        }

        /**
         * getfObjResSpaceSize
         *
         * @brief
         * 功能: 获取对象资源空间大小
         * 
         * @return
         * long: 对象资源空间大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfObjResSpaceSize() {
            return fObjResSpaceSize;
        }

        /**
         * setfObjResSpaceSize
         *
         * @brief
         * 功能: 设置对象资源空间大小
         * 
         * @param[in]  fObjResSpaceSize: 对象资源空间大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfObjResSpaceSize(long fObjResSpaceSize) {
            this.fObjResSpaceSize = fObjResSpaceSize;
        }

        /**
         * getfTaskExpStackSize
         *
         * @brief
         * 功能: 获取任务异常栈大小
         * 
         * @return
         * long: 任务异常栈大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfTaskExpStackSize() {
            return fTaskExpStackSize;
        }

        /**
         * setfTaskExpStackSize
         *
         * @brief
         * 功能: 设置任务异常栈大小
         * 
         * @param[in]  fTaskExpStackSize: 任务异常栈大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfTaskExpStackSize(long fTaskExpStackSize) {
            this.fTaskExpStackSize = fTaskExpStackSize;
        }

        /**
         * getfIdleTaskStackSize
         *
         * @brief
         * 功能: 获取空闲任务栈大小
         * 
         * @return
         * long: 空闲任务栈大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfIdleTaskStackSize() {
            return fIdleTaskStackSize;
        }

        /**
         * setfIdleTaskStackSize
         *
         * @brief
         * 功能: 设置空闲任务栈大小
         * 
         * @param[in]  fIdleTaskStackSize: 空闲任务栈大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfIdleTaskStackSize(long fIdleTaskStackSize) {
            this.fIdleTaskStackSize = fIdleTaskStackSize;
        }

        /**
         * getfInitTaskStackSize
         *
         * @brief
         * 功能: 获取初始化任务栈大小
         * 
         * @return
         * long: 初始化任务栈大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfInitTaskStackSize() {
            return fInitTaskStackSize;
        }

        /**
         * setfInitTaskStackSize
         *
         * @brief
         * 功能: 设置初始化任务栈大小
         * 
         * @param[in]  fInitTaskStackSize: 初始化任务栈大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfInitTaskStackSize(long fInitTaskStackSize) {
            this.fInitTaskStackSize = fInitTaskStackSize;
        }

        /**
         * getfInitTaskPriority
         *
         * @brief
         * 功能: 获取初始化任务优先级
         * 
         * @return
         * int: 初始化任务优先级
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfInitTaskPriority() {
            return fInitTaskPriority;
        }

        /**
         * setfInitTaskPriority
         *
         * @brief
         * 功能: 设置初始化任务优先级
         * 
         * @param[in]  fInitTaskPriority: 初始化任务优先级
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfInitTaskPriority(int fInitTaskPriority) {
            this.fInitTaskPriority = fInitTaskPriority;
        }

        /**
         * getfDefaultAppName
         *
         * @brief
         * 功能: 获取默认应用名称字符数组
         * 
         * @return
         * char[]: 默认应用名称字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getfDefaultAppName() {
            return fDefaultAppName;
        }

        /**
         * setfDefaultAppName
         *
         * @brief
         * 功能: 以字符数组设置默认应用名称
         * 
         * @param[in]  fDefaultAppName: 默认应用名称字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfDefaultAppName(char[] fDefaultAppName) {
            /* ... 拷贝逻辑 ... */ }

        /**
         * getfDefaultAppNameAsString
         *
         * @brief
         * 功能: 将默认应用名称字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 默认应用名称字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfDefaultAppNameAsString() {
            int endIndex = 0;
            while (endIndex < fDefaultAppName.length && fDefaultAppName[endIndex] != 0) {
                endIndex++;
            }
            byte[] bytes = new byte[endIndex];
            for (int i = 0; i < endIndex; i++) {
                bytes[i] = (byte) fDefaultAppName[i];
            }
            return new String(bytes, StandardCharsets.US_ASCII);
        }

        /**
         * setfDefaultAppName
         *
         * @brief
         * 功能: 以字符串设置默认应用名称，按US_ASCII编码写入字符数组
         * 
         * @param[in]  fDefaultAppName: 默认应用名称字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfDefaultAppName(String fDefaultAppName) {
            if (fDefaultAppName == null) {
                Arrays.fill(this.fDefaultAppName, (char) 0);
                return;
            }
            byte[] bytes = fDefaultAppName.getBytes(StandardCharsets.US_ASCII);
            int length = Math.min(bytes.length, this.fDefaultAppName.length);
            for (int i = 0; i < length; i++) {
                this.fDefaultAppName[i] = (char) bytes[i];
            }
            for (int i = length; i < this.fDefaultAppName.length; i++) {
                this.fDefaultAppName[i] = 0;
            }
        }

        /**
         * getfUserExtension1
         *
         * @brief
         * 功能: 获取用户扩展字段1
         * 
         * @return
         * int: 用户扩展字段1
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfUserExtension1() {
            return fUserExtension1;
        }

        /**
         * setfUserExtension1
         *
         * @brief
         * 功能: 设置用户扩展字段1
         * 
         * @param[in]  fUserExtension1: 用户扩展字段1
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUserExtension1(int fUserExtension1) {
            this.fUserExtension1 = fUserExtension1;
        }

        /**
         * getfUserExtension2
         *
         * @brief
         * 功能: 获取用户扩展字段2
         * 
         * @return
         * int: 用户扩展字段2
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfUserExtension2() {
            return fUserExtension2;
        }

        /**
         * setfUserExtension2
         *
         * @brief
         * 功能: 设置用户扩展字段2
         * 
         * @param[in]  fUserExtension2: 用户扩展字段2
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUserExtension2(int fUserExtension2) {
            this.fUserExtension2 = fUserExtension2;
        }
    }

}
