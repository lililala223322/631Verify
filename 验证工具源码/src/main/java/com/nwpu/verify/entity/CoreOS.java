/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreOS.java
 * 
 * 描述    : 核心操作系统配置实体类
 *           对应XML中的 /Module/CoreOS/CoreOSDescription 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.List;
import java.util.ArrayList;

public class CoreOS {

    /* 成员变量 */
    private char[] fKernelName = new char[32]; // 添加32位字符数组保存kernelName
    private KernelConfiguration fKernelConfiguration;
    private MemorySize fMemorySize;
    private HardwareConfiguration fHardwareConfiguration;

    /**
     * CoreOS
     *
     * @brief
     * 功能: 默认构造函数，初始化内核配置、内存大小及硬件配置对象
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreOS() {
        this.fKernelConfiguration = new KernelConfiguration();
        this.fMemorySize = new MemorySize();
        this.fHardwareConfiguration = new HardwareConfiguration();
    }

    /**
     * getKernelName
     *
     * @brief
     * 功能: 获取内核名称字符数组
     * 
     * @return
     * char[]: 内核名称字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getKernelName() {
        return fKernelName;
    }

    /**
     * setKernelName
     *
     * @brief
     * 功能: 以字符数组设置内核名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  kernelName: 内核名称字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setKernelName(char[] kernelName) {
        /* 确保不超过32个字符 */
        int length = Math.min(kernelName.length, this.fKernelName.length);
        for (int i = 0; i < length; i++) {
            this.fKernelName[i] = kernelName[i];
        }
        /* 如果输入字符数组长度小于32，其余部分填充为0 */
        for (int i = length; i < this.fKernelName.length; i++) {
            this.fKernelName[i] = 0;
        }
    }

    /**
     * setKernelName
     *
     * @brief
     * 功能: 通过字符串转字符数组设置内核名称
     * 
     * @param[in]  kernelName: 内核名称字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setKernelName(String kernelName) {
        char[] chars = kernelName.toCharArray();
        setKernelName(chars);
    }

    /**
     * getKernelNameAsString
     *
     * @brief
     * 功能: 字符数组转字符串，遇0终止符截断
     * 
     * @return
     * String: 内核名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getKernelNameAsString() {
        /* 找到第一个null字符的位置 */
        int endIndex = 0;
        while (endIndex < fKernelName.length && fKernelName[endIndex] != 0) {
            endIndex++;
        }
        return new String(fKernelName, 0, endIndex);
    }

    /**
     * getfKernelConfiguration
     *
     * @brief
     * 功能: 获取内核配置对象
     * 
     * @return
     * KernelConfiguration: 内核配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public KernelConfiguration getfKernelConfiguration() {
        return fKernelConfiguration;
    }

    /**
     * setfKernelConfiguration
     *
     * @brief
     * 功能: 设置内核配置对象
     * 
     * @param[in]  fKernelConfiguration: 内核配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfKernelConfiguration(KernelConfiguration fKernelConfiguration) {
        this.fKernelConfiguration = fKernelConfiguration;
    }

    /**
     * getfMemorySize
     *
     * @brief
     * 功能: 获取内存大小配置对象
     * 
     * @return
     * MemorySize: 内存大小配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public MemorySize getfMemorySize() {
        return fMemorySize;
    }

    /**
     * setfMemorySize
     *
     * @brief
     * 功能: 设置内存大小配置对象
     * 
     * @param[in]  fMemorySize: 内存大小配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMemorySize(MemorySize fMemorySize) {
        this.fMemorySize = fMemorySize;
    }

    /**
     * getfHardwareConfiguration
     *
     * @brief
     * 功能: 获取硬件配置对象
     * 
     * @return
     * HardwareConfiguration: 硬件配置对象
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public HardwareConfiguration getfHardwareConfiguration() {
        return fHardwareConfiguration;
    }

    /**
     * setfHardwareConfiguration
     *
     * @brief
     * 功能: 设置硬件配置对象
     * 
     * @param[in]  fHardwareConfiguration: 硬件配置对象
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfHardwareConfiguration(HardwareConfiguration fHardwareConfiguration) {
        this.fHardwareConfiguration = fHardwareConfiguration;
    }

    /**
     * KernelConfiguration
     *
     * @brief
     * 功能: 内部类：KernelConfiguration，内核配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class KernelConfiguration {
        private int fAddressSpaceRegionAllocationUnit;// 逻辑空间分配单位,pic7
        private long fAddressSpaceSize;// 逻辑空间大小,pic7
        private long fKernelVirtualAddress;// 段虚拟地址.数据词典pic61
        private int fAMemoryAllocationDisabled;// 动态空间分配禁止标志,pic7
        private long fPartitionVirtualAddress;// 分区逻辑空间地址,pic7
        private int fSysClkRateHz;// 系统时钟频率,pic7
        private long fUnsafeMethodTimeout;// 非安全系统调用超时时间 ,pic7
        private long fWatchDogTimerDuration;// 锁调度的时间,pic6
        private int fUser1;// 未配置则取128
        private int fUser2;// 未配置则取0
        private String fPayloadMapSize; // CPM部署信息可加载文件大小，支持十六进制/十进制格式
        private long fPartitionSpaceSize; // 分区空间大小，unsigned int

        /**
         * KernelConfiguration
         *
         * @brief
         * 功能: 默认构造函数
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public KernelConfiguration() {

        }

        /**
         * KernelConfiguration
         *
         * @brief
         * 功能: 带参构造函数，初始化内核配置各字段
         * 
         * @param[in]  fAddressSpaceRegionAllocationUnit: 逻辑空间分配单位
         * @param[in]  fAddressSpaceSize: 逻辑空间大小
         * @param[in]  fKernelVirtualAddress: 段虚拟地址
         * @param[in]  fAMemoryAllocationDisabled: 动态空间分配禁止标志
         * @param[in]  fPartitionVirtualAddress: 分区逻辑空间地址
         * @param[in]  fSysClkRateHz: 系统时钟频率
         * @param[in]  fUnsafeMethodTimeout: 非安全系统调用超时时间
         * @param[in]  fWatchDogTimerDuration: 锁调度的时间
         * @param[in]  fUser1: 用户字段1，未配置则取128
         * @param[in]  fUser2: 用户字段2，未配置则取0
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public KernelConfiguration(int fAddressSpaceRegionAllocationUnit, long fAddressSpaceSize,
                long fKernelVirtualAddress, int fAMemoryAllocationDisabled, long fPartitionVirtualAddress,
                int fSysClkRateHz, long fUnsafeMethodTimeout, long fWatchDogTimerDuration, int fUser1, int fUser2) {
            this.fAddressSpaceRegionAllocationUnit = fAddressSpaceRegionAllocationUnit;
            this.fAddressSpaceSize = fAddressSpaceSize;
            this.fKernelVirtualAddress = fKernelVirtualAddress;
            this.fAMemoryAllocationDisabled = fAMemoryAllocationDisabled;
            this.fPartitionVirtualAddress = fPartitionVirtualAddress;
            this.fSysClkRateHz = fSysClkRateHz;
            this.fUnsafeMethodTimeout = fUnsafeMethodTimeout;
            this.fWatchDogTimerDuration = fWatchDogTimerDuration;
            this.fUser1 = fUser1;
            this.fUser2 = fUser2;
        }

        /**
         * getfAddressSpaceRegionAllocationUnit
         *
         * @brief
         * 功能: 获取逻辑空间分配单位
         * 
         * @return
         * int: 逻辑空间分配单位
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfAddressSpaceRegionAllocationUnit() {
            return fAddressSpaceRegionAllocationUnit;
        }

        /**
         * setfAddressSpaceRegionAllocationUnit
         *
         * @brief
         * 功能: 设置逻辑空间分配单位
         * 
         * @param[in]  fAddressSpaceRegionAllocationUnit: 逻辑空间分配单位
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAddressSpaceRegionAllocationUnit(int fAddressSpaceRegionAllocationUnit) {
            this.fAddressSpaceRegionAllocationUnit = fAddressSpaceRegionAllocationUnit;
        }

        /**
         * getfAddressSpaceSize
         *
         * @brief
         * 功能: 获取逻辑空间大小
         * 
         * @return
         * long: 逻辑空间大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfAddressSpaceSize() {
            return fAddressSpaceSize;
        }

        /**
         * setfAddressSpaceSize
         *
         * @brief
         * 功能: 设置逻辑空间大小
         * 
         * @param[in]  fAddressSpaceSize: 逻辑空间大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAddressSpaceSize(long fAddressSpaceSize) {
            this.fAddressSpaceSize = fAddressSpaceSize;
        }

        /**
         * getfKernelVirtualAddress
         *
         * @brief
         * 功能: 获取段虚拟地址
         * 
         * @return
         * long: 段虚拟地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfKernelVirtualAddress() {
            return fKernelVirtualAddress;
        }

        /**
         * setfKernelVirtualAddress
         *
         * @brief
         * 功能: 设置段虚拟地址
         * 
         * @param[in]  fKernelVirtualAddress: 段虚拟地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfKernelVirtualAddress(long fKernelVirtualAddress) {
            this.fKernelVirtualAddress = fKernelVirtualAddress;
        }

        /**
         * getfAMemoryAllocationDisabled
         *
         * @brief
         * 功能: 获取动态空间分配禁止标志
         * 
         * @return
         * int: 动态空间分配禁止标志
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfAMemoryAllocationDisabled() {
            return fAMemoryAllocationDisabled;
        }

        /**
         * setfAMemoryAllocationDisabled
         *
         * @brief
         * 功能: 设置动态空间分配禁止标志
         * 
         * @param[in]  fAMemoryAllocationDisabled: 动态空间分配禁止标志
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfAMemoryAllocationDisabled(int fAMemoryAllocationDisabled) {
            this.fAMemoryAllocationDisabled = fAMemoryAllocationDisabled;
        }

        /**
         * getfPartitionVirtualAddress
         *
         * @brief
         * 功能: 获取分区逻辑空间地址
         * 
         * @return
         * long: 分区逻辑空间地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfPartitionVirtualAddress() {
            return fPartitionVirtualAddress;
        }

        /**
         * setfPartitionVirtualAddress
         *
         * @brief
         * 功能: 设置分区逻辑空间地址
         * 
         * @param[in]  fPartitionVirtualAddress: 分区逻辑空间地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPartitionVirtualAddress(long fPartitionVirtualAddress) {
            this.fPartitionVirtualAddress = fPartitionVirtualAddress;
        }

        /**
         * getfSysClkRateHz
         *
         * @brief
         * 功能: 获取系统时钟频率
         * 
         * @return
         * int: 系统时钟频率
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfSysClkRateHz() {
            return fSysClkRateHz;
        }

        /**
         * setfSysClkRateHz
         *
         * @brief
         * 功能: 设置系统时钟频率
         * 
         * @param[in]  fSysClkRateHz: 系统时钟频率
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSysClkRateHz(int fSysClkRateHz) {
            this.fSysClkRateHz = fSysClkRateHz;
        }

        /**
         * getfUnsafeMethodTimeout
         *
         * @brief
         * 功能: 获取非安全系统调用超时时间
         * 
         * @return
         * long: 非安全系统调用超时时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfUnsafeMethodTimeout() {
            return fUnsafeMethodTimeout;
        }

        /**
         * setfUnsafeMethodTimeout
         *
         * @brief
         * 功能: 设置非安全系统调用超时时间
         * 
         * @param[in]  fUnsafeMethodTimeout: 非安全系统调用超时时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUnsafeMethodTimeout(long fUnsafeMethodTimeout) {
            this.fUnsafeMethodTimeout = fUnsafeMethodTimeout;
        }

        /**
         * getfWatchDogTimerDuration
         *
         * @brief
         * 功能: 获取锁调度的时间
         * 
         * @return
         * long: 锁调度的时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfWatchDogTimerDuration() {
            return fWatchDogTimerDuration;
        }

        /**
         * setfWatchDogTimerDuration
         *
         * @brief
         * 功能: 设置锁调度的时间
         * 
         * @param[in]  fWatchDogTimerDuration: 锁调度的时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfWatchDogTimerDuration(long fWatchDogTimerDuration) {
            this.fWatchDogTimerDuration = fWatchDogTimerDuration;
        }

        /**
         * getfUser1
         *
         * @brief
         * 功能: 获取用户字段1，未配置则取128
         * 
         * @return
         * int: 用户字段1
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfUser1() {
            return fUser1;
        }

        /**
         * setfUser1
         *
         * @brief
         * 功能: 设置用户字段1
         * 
         * @param[in]  fUser1: 用户字段1
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUser1(int fUser1) {
            this.fUser1 = fUser1;
        }

        /**
         * getfUser2
         *
         * @brief
         * 功能: 获取用户字段2，未配置则取0
         * 
         * @return
         * int: 用户字段2
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getfUser2() {
            return fUser2;
        }

        /**
         * setfUser2
         *
         * @brief
         * 功能: 设置用户字段2
         * 
         * @param[in]  fUser2: 用户字段2
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUser2(int fUser2) {
            this.fUser2 = fUser2;
        }

        /**
         * getfPayloadMapSize
         *
         * @brief
         * 功能: 获取CPM部署信息可加载文件大小，支持十六进制/十进制格式
         * 
         * @return
         * String: CPM部署信息可加载文件大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfPayloadMapSize() {
            return fPayloadMapSize;
        }

        /**
         * setfPayloadMapSize
         *
         * @brief
         * 功能: 设置CPM部署信息可加载文件大小，支持十六进制/十进制格式
         * 
         * @param[in]  fPayloadMapSize: CPM部署信息可加载文件大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPayloadMapSize(String fPayloadMapSize) {
            this.fPayloadMapSize = fPayloadMapSize;
        }

        /**
         * getfPartitionSpaceSize
         *
         * @brief
         * 功能: 获取分区空间大小
         * 
         * @return
         * long: 分区空间大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfPartitionSpaceSize() {
            return fPartitionSpaceSize;
        }

        /**
         * setfPartitionSpaceSize
         *
         * @brief
         * 功能: 设置分区空间大小
         * 
         * @param[in]  fPartitionSpaceSize: 分区空间大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPartitionSpaceSize(long fPartitionSpaceSize) {
            this.fPartitionSpaceSize = fPartitionSpaceSize;
        }
    }

    /**
     * MemorySize
     *
     * @brief
     * 功能: 内部类：MemorySize，内存大小配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class MemorySize {
        private long fMemorySizeBss;// 段大小
        private long fMemorySizeData;// 段大小,pic64
        private long fMemorySizeRoData;// 段大小,pic62
        private long fMemorySizeText;// 段大小,pic61

        /**
         * MemorySize
         *
         * @brief
         * 功能: 默认构造函数
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public MemorySize() {

        }

        /**
         * MemorySize
         *
         * @brief
         * 功能: 带参构造函数，初始化各段内存大小
         * 
         * @param[in]  fMemorySizeBss: Bss段大小
         * @param[in]  fMemorySizeData: Data段大小
         * @param[in]  fMemorySizeRoData: RoData段大小
         * @param[in]  fMemorySizeText: Text段大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public MemorySize(long fMemorySizeBss, long fMemorySizeData, long fMemorySizeRoData, long fMemorySizeText) {
            this.fMemorySizeBss = fMemorySizeBss;
            this.fMemorySizeData = fMemorySizeData;
            this.fMemorySizeRoData = fMemorySizeRoData;
            this.fMemorySizeText = fMemorySizeText;
        }

        /**
         * getfMemorySizeBss
         *
         * @brief
         * 功能: 获取Bss段大小
         * 
         * @return
         * long: Bss段大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizeBss() {
            return fMemorySizeBss;
        }

        /**
         * setfMemorySizeBss
         *
         * @brief
         * 功能: 设置Bss段大小
         * 
         * @param[in]  fMemorySizeBss: Bss段大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizeBss(long fMemorySizeBss) {
            this.fMemorySizeBss = fMemorySizeBss;
        }

        /**
         * getfMemorySizeData
         *
         * @brief
         * 功能: 获取Data段大小
         * 
         * @return
         * long: Data段大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizeData() {
            return fMemorySizeData;
        }

        /**
         * setfMemorySizeData
         *
         * @brief
         * 功能: 设置Data段大小
         * 
         * @param[in]  fMemorySizeData: Data段大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizeData(long fMemorySizeData) {
            this.fMemorySizeData = fMemorySizeData;
        }

        /**
         * getfMemorySizeRoData
         *
         * @brief
         * 功能: 获取RoData段大小
         * 
         * @return
         * long: RoData段大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizeRoData() {
            return fMemorySizeRoData;
        }

        /**
         * setfMemorySizeRoData
         *
         * @brief
         * 功能: 设置RoData段大小
         * 
         * @param[in]  fMemorySizeRoData: RoData段大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizeRoData(long fMemorySizeRoData) {
            this.fMemorySizeRoData = fMemorySizeRoData;
        }

        /**
         * getfMemorySizeText
         *
         * @brief
         * 功能: 获取Text段大小
         * 
         * @return
         * long: Text段大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizeText() {
            return fMemorySizeText;
        }

        /**
         * setfMemorySizeText
         *
         * @brief
         * 功能: 设置Text段大小
         * 
         * @param[in]  fMemorySizeText: Text段大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizeText(long fMemorySizeText) {
            this.fMemorySizeText = fMemorySizeText;
        }
    }

    /**
     * HardwareConfiguration
     *
     * @brief
     * 功能: 内部类：HardwareConfiguration，硬件配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class HardwareConfiguration {
        private PhysicalMemory fPhysicalMemory;
        private UserMemoryRegion fUserMemoryRegion;
        private List<KernelIO> fKernelIOList;
        private PayloadMemory fPayloadMemory;
        private SharedIO fSharedIO; // 新增：sharedIO支持

        /**
         * HardwareConfiguration
         *
         * @brief
         * 功能: 默认构造函数，初始化内核IO列表
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public HardwareConfiguration() {
            this.fKernelIOList = new ArrayList<>();
        }

        /**
         * getfPhysicalMemory
         *
         * @brief
         * 功能: 获取物理内存配置对象
         * 
         * @return
         * PhysicalMemory: 物理内存配置对象
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public PhysicalMemory getfPhysicalMemory() {
            return fPhysicalMemory;
        }

        /**
         * setfPhysicalMemory
         *
         * @brief
         * 功能: 设置物理内存配置对象
         * 
         * @param[in]  fPhysicalMemory: 物理内存配置对象
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPhysicalMemory(PhysicalMemory fPhysicalMemory) {
            this.fPhysicalMemory = fPhysicalMemory;
        }

        /**
         * getfUserMemoryRegion
         *
         * @brief
         * 功能: 获取用户内存区域配置对象
         * 
         * @return
         * UserMemoryRegion: 用户内存区域配置对象
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public UserMemoryRegion getfUserMemoryRegion() {
            return fUserMemoryRegion;
        }

        /**
         * setfUserMemoryRegion
         *
         * @brief
         * 功能: 设置用户内存区域配置对象
         * 
         * @param[in]  fUserMemoryRegion: 用户内存区域配置对象
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfUserMemoryRegion(UserMemoryRegion fUserMemoryRegion) {
            this.fUserMemoryRegion = fUserMemoryRegion;
        }

        /**
         * getfKernelIOList
         *
         * @brief
         * 功能: 获取内核IO列表
         * 
         * @return
         * List<KernelIO>: 内核IO列表
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public List<KernelIO> getfKernelIOList() {
            return fKernelIOList;
        }

        /**
         * setfKernelIOList
         *
         * @brief
         * 功能: 设置内核IO列表
         * 
         * @param[in]  fKernelIOList: 内核IO列表
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfKernelIOList(List<KernelIO> fKernelIOList) {
            this.fKernelIOList = fKernelIOList;
        }

        /**
         * addKernelIO
         *
         * @brief
         * 功能: 向内核IO列表添加一个内核IO配置
         * 
         * @param[in]  kernelIO: 内核IO配置
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void addKernelIO(KernelIO kernelIO) {
            this.fKernelIOList.add(kernelIO);
        }

        /**
         * getfPayloadMemory
         *
         * @brief
         * 功能: 获取Payload内存配置对象
         * 
         * @return
         * PayloadMemory: Payload内存配置对象
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public PayloadMemory getfPayloadMemory() {
            return fPayloadMemory;
        }

        /**
         * setfPayloadMemory
         *
         * @brief
         * 功能: 设置Payload内存配置对象
         * 
         * @param[in]  fPayloadMemory: Payload内存配置对象
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfPayloadMemory(PayloadMemory fPayloadMemory) {
            this.fPayloadMemory = fPayloadMemory;
        }

        /**
         * getfSharedIO
         *
         * @brief
         * 功能: 获取SharedIO配置对象
         * 
         * @return
         * SharedIO: SharedIO配置对象
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public SharedIO getfSharedIO() {
            return fSharedIO;
        }

        /**
         * setfSharedIO
         *
         * @brief
         * 功能: 设置SharedIO配置对象
         * 
         * @param[in]  fSharedIO: SharedIO配置对象
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSharedIO(SharedIO fSharedIO) {
            this.fSharedIO = fSharedIO;
        }

        /**
         * SharedIO
         *
         * @brief
         * 功能: 内部类：SharedIO，对应coreOS.xml中的sharedIO元素
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class SharedIO {
            private long fBaseAddress;
            private String fCachePolicy;
            private String fPoolName;
            private long fSize;
            private long fVirtualAddress;

            /**
             * SharedIO
             *
             * @brief
             * 功能: 默认构造函数
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public SharedIO() {
                /* 默认构造函数 */
            }

            /**
             * getfBaseAddress
             *
             * @brief
             * 功能: 获取基地址
             * 
             * @return
             * long: 基地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfBaseAddress() {
                return fBaseAddress;
            }

            /**
             * setfBaseAddress
             *
             * @brief
             * 功能: 设置基地址
             * 
             * @param[in]  fBaseAddress: 基地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfBaseAddress(long fBaseAddress) {
                this.fBaseAddress = fBaseAddress;
            }

            /**
             * getfCachePolicy
             *
             * @brief
             * 功能: 获取缓存策略
             * 
             * @return
             * String: 缓存策略
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfCachePolicy() {
                return fCachePolicy;
            }

            /**
             * setfCachePolicy
             *
             * @brief
             * 功能: 设置缓存策略
             * 
             * @param[in]  fCachePolicy: 缓存策略
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfCachePolicy(String fCachePolicy) {
                this.fCachePolicy = fCachePolicy;
            }

            /**
             * getfPoolName
             *
             * @brief
             * 功能: 获取内存池名称
             * 
             * @return
             * String: 内存池名称
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfPoolName() {
                return fPoolName;
            }

            /**
             * setfPoolName
             *
             * @brief
             * 功能: 设置内存池名称
             * 
             * @param[in]  fPoolName: 内存池名称
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfPoolName(String fPoolName) {
                this.fPoolName = fPoolName;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取大小
             * 
             * @return
             * long: 大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置大小
             * 
             * @param[in]  fSize: 大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(long fSize) {
                this.fSize = fSize;
            }

            /**
             * getfVirtualAddress
             *
             * @brief
             * 功能: 获取虚拟地址
             * 
             * @return
             * long: 虚拟地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfVirtualAddress() {
                return fVirtualAddress;
            }

            /**
             * setfVirtualAddress
             *
             * @brief
             * 功能: 设置虚拟地址
             * 
             * @param[in]  fVirtualAddress: 虚拟地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfVirtualAddress(long fVirtualAddress) {
                this.fVirtualAddress = fVirtualAddress;
            }
        }

        /**
         * PhysicalMemory
         *
         * @brief
         * 功能: 内部类：PhysicalMemory，物理内存配置信息
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class PhysicalMemory {
            private long fBaseAddress;// 物理/虚拟地址,pic30
            private long fSize;
            private long fKernelMemoryRegionSize;
            private String fKernelConfigRecordRegionSize;
            private long fKernelPgPoolSize;// 核心页内存块空间配置大小,pic30
            private long fPortRegionSize;// 端口缓冲区内存块空间配置之空间大小,pic31
            private long fHmLogRegionSize;// 健康监控日志记录内存块空间大小,pic31
            private String fRamPayloadRegionBaseAddress;
            private String fRamPayloadRegionSize;
            private String fLibRegionBaseAddress;
            private String fLibRegionSize;
            private List<KernelRegion> fKernelRegions; // 新增：kernelRegion支持

            /**
             * PhysicalMemory
             *
             * @brief
             * 功能: 默认构造函数，初始化内核区域列表
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public PhysicalMemory() {
                /* 默认构造函数 */
                this.fKernelRegions = new ArrayList<>();
            }

            /**
             * getfBaseAddress
             *
             * @brief
             * 功能: 获取物理/虚拟地址
             * 
             * @return
             * long: 物理/虚拟地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfBaseAddress() {
                return fBaseAddress;
            }

            /**
             * setfBaseAddress
             *
             * @brief
             * 功能: 设置物理/虚拟地址
             * 
             * @param[in]  fBaseAddress: 物理/虚拟地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfBaseAddress(long fBaseAddress) {
                this.fBaseAddress = fBaseAddress;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取大小
             * 
             * @return
             * long: 大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置大小
             * 
             * @param[in]  fSize: 大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(long fSize) {
                this.fSize = fSize;
            }

            /**
             * getfKernelMemoryRegionSize
             *
             * @brief
             * 功能: 获取内核内存区域大小
             * 
             * @return
             * long: 内核内存区域大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfKernelMemoryRegionSize() {
                return fKernelMemoryRegionSize;
            }

            /**
             * setfKernelMemoryRegionSize
             *
             * @brief
             * 功能: 设置内核内存区域大小
             * 
             * @param[in]  fKernelMemoryRegionSize: 内核内存区域大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfKernelMemoryRegionSize(long fKernelMemoryRegionSize) {
                this.fKernelMemoryRegionSize = fKernelMemoryRegionSize;
            }

            /**
             * getfKernelConfigRecordRegionSize
             *
             * @brief
             * 功能: 获取内核配置记录区域大小
             * 
             * @return
             * String: 内核配置记录区域大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfKernelConfigRecordRegionSize() {
                return fKernelConfigRecordRegionSize;
            }

            /**
             * setfKernelConfigRecordRegionSize
             *
             * @brief
             * 功能: 设置内核配置记录区域大小
             * 
             * @param[in]  fKernelConfigRecordRegionSize: 内核配置记录区域大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfKernelConfigRecordRegionSize(String fKernelConfigRecordRegionSize) {
                this.fKernelConfigRecordRegionSize = fKernelConfigRecordRegionSize;
            }

            /**
             * getfKernelPgPoolSize
             *
             * @brief
             * 功能: 获取核心页内存块空间配置大小
             * 
             * @return
             * long: 核心页内存块空间配置大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfKernelPgPoolSize() {
                return fKernelPgPoolSize;
            }

            /**
             * setfKernelPgPoolSize
             *
             * @brief
             * 功能: 设置核心页内存块空间配置大小
             * 
             * @param[in]  fKernelPgPoolSize: 核心页内存块空间配置大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfKernelPgPoolSize(long fKernelPgPoolSize) {
                this.fKernelPgPoolSize = fKernelPgPoolSize;
            }

            /**
             * getfPortRegionSize
             *
             * @brief
             * 功能: 获取端口缓冲区内存块空间配置之空间大小
             * 
             * @return
             * long: 端口缓冲区内存块空间配置之空间大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfPortRegionSize() {
                return fPortRegionSize;
            }

            /**
             * setfPortRegionSize
             *
             * @brief
             * 功能: 设置端口缓冲区内存块空间配置之空间大小
             * 
             * @param[in]  fPortRegionSize: 端口缓冲区内存块空间配置之空间大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfPortRegionSize(long fPortRegionSize) {
                this.fPortRegionSize = fPortRegionSize;
            }

            /**
             * getfHmLogRegionSize
             *
             * @brief
             * 功能: 获取健康监控日志记录内存块空间大小
             * 
             * @return
             * long: 健康监控日志记录内存块空间大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfHmLogRegionSize() {
                return fHmLogRegionSize;
            }

            /**
             * setfHmLogRegionSize
             *
             * @brief
             * 功能: 设置健康监控日志记录内存块空间大小
             * 
             * @param[in]  fHmLogRegionSize: 健康监控日志记录内存块空间大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfHmLogRegionSize(long fHmLogRegionSize) {
                this.fHmLogRegionSize = fHmLogRegionSize;
            }

            /**
             * getfRamPayloadRegionBaseAddress
             *
             * @brief
             * 功能: 获取RAM Payload区域基地址
             * 
             * @return
             * String: RAM Payload区域基地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfRamPayloadRegionBaseAddress() {
                return fRamPayloadRegionBaseAddress;
            }

            /**
             * setfRamPayloadRegionBaseAddress
             *
             * @brief
             * 功能: 设置RAM Payload区域基地址
             * 
             * @param[in]  fRamPayloadRegionBaseAddress: RAM Payload区域基地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfRamPayloadRegionBaseAddress(String fRamPayloadRegionBaseAddress) {
                this.fRamPayloadRegionBaseAddress = fRamPayloadRegionBaseAddress;
            }

            /**
             * getfRamPayloadRegionSize
             *
             * @brief
             * 功能: 获取RAM Payload区域大小
             * 
             * @return
             * String: RAM Payload区域大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfRamPayloadRegionSize() {
                return fRamPayloadRegionSize;
            }

            /**
             * setfRamPayloadRegionSize
             *
             * @brief
             * 功能: 设置RAM Payload区域大小
             * 
             * @param[in]  fRamPayloadRegionSize: RAM Payload区域大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfRamPayloadRegionSize(String fRamPayloadRegionSize) {
                this.fRamPayloadRegionSize = fRamPayloadRegionSize;
            }

            /**
             * getfLibRegionBaseAddress
             *
             * @brief
             * 功能: 获取库区域基地址
             * 
             * @return
             * String: 库区域基地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfLibRegionBaseAddress() {
                return fLibRegionBaseAddress;
            }

            /**
             * setfLibRegionBaseAddress
             *
             * @brief
             * 功能: 设置库区域基地址
             * 
             * @param[in]  fLibRegionBaseAddress: 库区域基地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfLibRegionBaseAddress(String fLibRegionBaseAddress) {
                this.fLibRegionBaseAddress = fLibRegionBaseAddress;
            }

            /**
             * getfLibRegionSize
             *
             * @brief
             * 功能: 获取库区域大小
             * 
             * @return
             * String: 库区域大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfLibRegionSize() {
                return fLibRegionSize;
            }

            /**
             * setfLibRegionSize
             *
             * @brief
             * 功能: 设置库区域大小
             * 
             * @param[in]  fLibRegionSize: 库区域大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfLibRegionSize(String fLibRegionSize) {
                this.fLibRegionSize = fLibRegionSize;
            }

            /**
             * getfKernelRegions
             *
             * @brief
             * 功能: 获取内核区域列表
             * 
             * @return
             * List<KernelRegion>: 内核区域列表
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public List<KernelRegion> getfKernelRegions() {
                return fKernelRegions;
            }

            /**
             * setfKernelRegions
             *
             * @brief
             * 功能: 设置内核区域列表
             * 
             * @param[in]  fKernelRegions: 内核区域列表
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfKernelRegions(List<KernelRegion> fKernelRegions) {
                this.fKernelRegions = fKernelRegions;
            }

            /**
             * addKernelRegion
             *
             * @brief
             * 功能: 向内核区域列表添加一个内核区域配置
             * 
             * @param[in]  kernelRegion: 内核区域配置
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void addKernelRegion(KernelRegion kernelRegion) {
                this.fKernelRegions.add(kernelRegion);
            }
        }

        /**
         * UserMemoryRegion
         *
         * @brief
         * 功能: 内部类：UserMemoryRegion，用户内存区域配置信息
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class UserMemoryRegion {
            private long fBaseAddress;// 物理地址,pic27
            private long fSize;// 大小，pic39

            /**
             * UserMemoryRegion
             *
             * @brief
             * 功能: 默认构造函数
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public UserMemoryRegion() {
                /* 默认构造函数 */
            }

            /**
             * getfBaseAddress
             *
             * @brief
             * 功能: 获取物理地址
             * 
             * @return
             * long: 物理地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfBaseAddress() {
                return fBaseAddress;
            }

            /**
             * setfBaseAddress
             *
             * @brief
             * 功能: 设置物理地址
             * 
             * @param[in]  fBaseAddress: 物理地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfBaseAddress(long fBaseAddress) {
                this.fBaseAddress = fBaseAddress;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取大小
             * 
             * @return
             * long: 大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置大小
             * 
             * @param[in]  fSize: 大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(long fSize) {
                this.fSize = fSize;
            }
        }

        /**
         * KernelIO
         *
         * @brief
         * 功能: 内部类：KernelIO，内核IO配置信息
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class KernelIO {
            private long fBaseAddress;
            private String fCachePolicy;
            private char[] fPoolName = new char[32];
            private long fSize;
            private long fVirtualAddress;

            /**
             * KernelIO
             *
             * @brief
             * 功能: 默认构造函数
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public KernelIO() {
                /* 默认构造函数 */
            }

            /**
             * setKernelName
             *
             * @brief
             * 功能: 以字符数组设置内存池名称，超出长度部分截断，不足部分以0填充
             * 
             * @param[in]  kernelName: 内存池名称字符数组
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setKernelName(char[] kernelName) {
                /* 确保不超过32个字符 */
                int length = Math.min(kernelName.length, this.fPoolName.length);
                for (int i = 0; i < length; i++) {
                    this.fPoolName[i] = kernelName[i];
                }
                /* 如果输入字符数组长度小于32，其余部分填充为0 */
                for (int i = length; i < this.fPoolName.length; i++) {
                    this.fPoolName[i] = 0;
                }
            }

            /**
             * setKernelName
             *
             * @brief
             * 功能: 通过字符串转字符数组设置内存池名称
             * 
             * @param[in]  kernelName: 内存池名称字符串
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setKernelName(String kernelName) {
                char[] chars = kernelName.toCharArray();
                setKernelName(chars);
            }

            /**
             * getKernelNameAsString
             *
             * @brief
             * 功能: 字符数组转字符串，遇0终止符截断
             * 
             * @return
             * String: 内存池名称字符串
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getKernelNameAsString() {
                /* 找到第一个null字符的位置 */
                int endIndex = 0;
                while (endIndex < fPoolName.length && fPoolName[endIndex] != 0) {
                    endIndex++;
                }
                return new String(fPoolName, 0, endIndex);
            }

            /**
             * getfBaseAddress
             *
             * @brief
             * 功能: 获取基地址
             * 
             * @return
             * long: 基地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfBaseAddress() {
                return fBaseAddress;
            }

            /**
             * setfBaseAddress
             *
             * @brief
             * 功能: 设置基地址
             * 
             * @param[in]  fBaseAddress: 基地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfBaseAddress(long fBaseAddress) {
                this.fBaseAddress = fBaseAddress;
            }

            /**
             * getfCachePolicy
             *
             * @brief
             * 功能: 获取缓存策略
             * 
             * @return
             * String: 缓存策略
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfCachePolicy() {
                return fCachePolicy;
            }

            /**
             * setfCachePolicy
             *
             * @brief
             * 功能: 设置缓存策略
             * 
             * @param[in]  fCachePolicy: 缓存策略
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfCachePolicy(String fCachePolicy) {
                this.fCachePolicy = fCachePolicy;
            }

            /**
             * getfPoolName
             *
             * @brief
             * 功能: 获取内存池名称字符数组
             * 
             * @return
             * char[]: 内存池名称字符数组
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public char[] getfPoolName() {
                return fPoolName;
            }

            /**
             * setfPoolName
             *
             * @brief
             * 功能: 设置内存池名称字符数组
             * 
             * @param[in]  fPoolName: 内存池名称字符数组
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfPoolName(char[] fPoolName) {
                this.fPoolName = fPoolName;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取大小
             * 
             * @return
             * long: 大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置大小
             * 
             * @param[in]  fSize: 大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(long fSize) {
                this.fSize = fSize;
            }

            /**
             * getfVirtualAddress
             *
             * @brief
             * 功能: 获取虚拟地址
             * 
             * @return
             * long: 虚拟地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfVirtualAddress() {
                return fVirtualAddress;
            }

            /**
             * setfVirtualAddress
             *
             * @brief
             * 功能: 设置虚拟地址
             * 
             * @param[in]  fVirtualAddress: 虚拟地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfVirtualAddress(long fVirtualAddress) {
                this.fVirtualAddress = fVirtualAddress;
            }
        }

        /**
         * PayloadMemory
         *
         * @brief
         * 功能: 内部类：PayloadMemory，Payload内存配置信息
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class PayloadMemory {
            private long fBaseAddress;
            private long fSize;

            /**
             * PayloadMemory
             *
             * @brief
             * 功能: 默认构造函数
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public PayloadMemory() {
                /* 默认构造函数 */
            }

            /**
             * getfBaseAddress
             *
             * @brief
             * 功能: 获取基地址
             * 
             * @return
             * long: 基地址
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfBaseAddress() {
                return fBaseAddress;
            }

            /**
             * setfBaseAddress
             *
             * @brief
             * 功能: 设置基地址
             * 
             * @param[in]  fBaseAddress: 基地址
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfBaseAddress(long fBaseAddress) {
                this.fBaseAddress = fBaseAddress;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取大小
             * 
             * @return
             * long: 大小
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置大小
             * 
             * @param[in]  fSize: 大小
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(long fSize) {
                this.fSize = fSize;
            }
        }

        /**
         * KernelRegion
         *
         * @brief
         * 功能: 内部类：KernelRegion，对应XML中的kernelRegion元素
         *       驻留在核心操作系统的用户自定义空间配置
         * 
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        public static class KernelRegion {
            private String fSize; // Size属性：驻留在核心操作系统的用户自定义空间大小（十进制或十六进制）
            private boolean fWriteProtected; // WriteProtected属性：写保护标记（布尔值）
            private String fPoolName; // PoolName属性：空间名称（1~30个字符）

            /**
             * KernelRegion
             *
             * @brief
             * 功能: 默认构造函数
             * 
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public KernelRegion() {
                /* 默认构造函数 */
                /* 默认不写保护 */
                this.fWriteProtected = false;
            }

            /**
             * KernelRegion
             *
             * @brief
             * 功能: 带参构造函数，初始化内核区域各属性
             * 
             * @param[in]  size: Size属性，驻留在核心操作系统的用户自定义空间大小（十进制或十六进制）
             * @param[in]  writeProtected: WriteProtected属性，写保护标记（布尔值）
             * @param[in]  poolName: PoolName属性，空间名称（1~30个字符）
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public KernelRegion(String size, boolean writeProtected, String poolName) {
                this.fSize = size;
                this.fWriteProtected = writeProtected;
                this.fPoolName = poolName;
            }

            /**
             * getfSize
             *
             * @brief
             * 功能: 获取Size属性，驻留在核心操作系统的用户自定义空间大小（十进制或十六进制）
             * 
             * @return
             * String: Size属性值
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfSize() {
                return fSize;
            }

            /**
             * setfSize
             *
             * @brief
             * 功能: 设置Size属性，驻留在核心操作系统的用户自定义空间大小（十进制或十六进制）
             * 
             * @param[in]  fSize: Size属性值
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfSize(String fSize) {
                this.fSize = fSize;
            }

            /**
             * getSizeAsLong
             *
             * @brief
             * 功能: 获取Size属性的长整型值，支持十进制和十六进制格式解析
             * 
             * @return
             * long: 解析后的长整型值，解析失败返回0
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public long getSizeAsLong() {
                if (fSize == null || fSize.trim().isEmpty()) {
                    return 0L;
                }

                String sizeStr = fSize.trim();
                try {
                    if (sizeStr.toLowerCase().startsWith("0x")) {
                        /* 十六进制格式 */
                        return Long.parseLong(sizeStr.substring(2), 16);
                    } else {
                        /* 十进制格式 */
                        return Long.parseLong(sizeStr);
                    }
                } catch (NumberFormatException e) {
                    /* 解析失败返回0 */
                    return 0L;
                }
            }

            /**
             * isfWriteProtected
             *
             * @brief
             * 功能: 获取WriteProtected属性，写保护标记（布尔值）
             * 
             * @return
             * boolean: 写保护标记
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public boolean isfWriteProtected() {
                return fWriteProtected;
            }

            /**
             * setfWriteProtected
             *
             * @brief
             * 功能: 设置WriteProtected属性，写保护标记（布尔值）
             * 
             * @param[in]  fWriteProtected: 写保护标记
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfWriteProtected(boolean fWriteProtected) {
                this.fWriteProtected = fWriteProtected;
            }

            /**
             * getfPoolName
             *
             * @brief
             * 功能: 获取PoolName属性，空间名称（1~30个字符）
             * 
             * @return
             * String: PoolName属性值
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public String getfPoolName() {
                return fPoolName;
            }

            /**
             * setfPoolName
             *
             * @brief
             * 功能: 设置PoolName属性，空间名称（1~30个字符）
             * 
             * @param[in]  fPoolName: PoolName属性值
             * @return
             * N/A
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public void setfPoolName(String fPoolName) {
                this.fPoolName = fPoolName;
            }

            /**
             * isPoolNameValid
             *
             * @brief
             * 功能: 验证PoolName是否符合约束（1~30个字符）
             * 
             * @return
             * boolean: true如果符合约束，false如果不符合
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            public boolean isPoolNameValid() {
                return fPoolName != null &&
                        fPoolName.length() >= 1 &&
                        fPoolName.length() <= 30;
            }

            /**
             * toString
             *
             * @brief
             * 功能: 返回对象的字符串表示
             * 
             * @return
             * String: 对象信息字符串
             * @implements 
             * @scope external
             * @errno:
             * N/A
             */
            @Override
            public String toString() {
                return "KernelRegion{" +
                        "size='" + fSize + '\'' +
                        ", writeProtected=" + fWriteProtected +
                        ", poolName='" + fPoolName + '\'' +
                        '}';
            }
        }
    }
}
