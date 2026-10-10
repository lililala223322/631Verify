/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ApplicationEntity.java
 * 
 * 描述    : 应用实体类，映射XML配置中的应用节点及其内存和端口信息
 * 
 * 修改历史:
 *     2026-06-07  开发  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.List;

public class ApplicationEntity {

    /* 成员变量 */
    private char[] fName = new char[32];
    private MemorySize fMemorySize;
    private List<QueuingPort> fQueuingPorts;
    private List<SamplingPort> fSamplingPorts;
    
    /**
     * ApplicationEntity
     *
     * @brief
     * 功能: 默认构造函数，初始化内存大小对象及端口列表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public ApplicationEntity() {
        this.fMemorySize = new MemorySize();
        this.fQueuingPorts = new ArrayList<>();
        this.fSamplingPorts = new ArrayList<>();
    }
    
    /**
     * getfName
     *
     * @brief
     * 功能: 获取应用名称字符数组
     * 
     * @return
     * char[]: 应用名称字符数组
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
     * 功能: 以字符数组设置应用名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: 应用名称字符数组
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
     * 功能: 以字符串设置应用名称
     * 
     * @param[in]  fName: 应用名称字符串
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
     * 功能: 将应用名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 应用名称字符串
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
     * getfMemorySize
     *
     * @brief
     * 功能: 获取应用内存大小配置对象
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
     * 功能: 设置应用内存大小配置对象
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
     * getfQueuingPorts
     *
     * @brief
     * 功能: 获取应用关联的排队端口列表
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
     * addQueuingPort
     *
     * @brief
     * 功能: 向应用添加一个排队端口
     * 
     * @param[in]  queuingPort: 待添加的排队端口
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
     * 功能: 获取应用关联的采样端口列表
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
     * addSamplingPort
     *
     * @brief
     * 功能: 向应用添加一个采样端口
     * 
     * @param[in]  samplingPort: 待添加的采样端口
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
     * MemorySize
     *
     * @brief
     * 功能: 应用内存大小内部类，映射各段内存配置及持久化段配置标志
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class MemorySize {
        private long fMemorySizeBss;
        private long fMemorySizeData;
        private long fMemorySizeRoData;
        private long fMemorySizeText;
        private long fMemorySizePersistentData;
        private long fMemorySizePersistentBss;
        
        /* 标志位：记录XML中是否配置了这些属性 */
        private boolean hasPersistentData = false;
        private boolean hasPersistentBss = false;
        
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
         * hasPersistentData
         *
         * @brief
         * 功能: 判断XML中是否配置了PersistentData属性
         * 
         * @return
         * boolean: 已配置返回true，否则返回false
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean hasPersistentData() {
            return hasPersistentData;
        }
        
        /**
         * setHasPersistentData
         *
         * @brief
         * 功能: 设置PersistentData属性配置标志
         * 
         * @param[in]  hasPersistentData: 是否已配置PersistentData
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setHasPersistentData(boolean hasPersistentData) {
            this.hasPersistentData = hasPersistentData;
        }
        
        /**
         * hasPersistentBss
         *
         * @brief
         * 功能: 判断XML中是否配置了PersistentBss属性
         * 
         * @return
         * boolean: 已配置返回true，否则返回false
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean hasPersistentBss() {
            return hasPersistentBss;
        }
        
        /**
         * setHasPersistentBss
         *
         * @brief
         * 功能: 设置PersistentBss属性配置标志
         * 
         * @param[in]  hasPersistentBss: 是否已配置PersistentBss
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setHasPersistentBss(boolean hasPersistentBss) {
            this.hasPersistentBss = hasPersistentBss;
        }
        
        /**
         * getfMemorySizeBss
         *
         * @brief
         * 功能: 获取Bss段内存大小
         * 
         * @return
         * long: Bss段内存大小
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
         * 功能: 设置Bss段内存大小
         * 
         * @param[in]  fMemorySizeBss: Bss段内存大小
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
         * 功能: 获取Data段内存大小
         * 
         * @return
         * long: Data段内存大小
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
         * 功能: 设置Data段内存大小
         * 
         * @param[in]  fMemorySizeData: Data段内存大小
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
         * 功能: 获取RoData段内存大小
         * 
         * @return
         * long: RoData段内存大小
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
         * 功能: 设置RoData段内存大小
         * 
         * @param[in]  fMemorySizeRoData: RoData段内存大小
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
         * 功能: 获取Text段内存大小
         * 
         * @return
         * long: Text段内存大小
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
         * 功能: 设置Text段内存大小
         * 
         * @param[in]  fMemorySizeText: Text段内存大小
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
        
        /**
         * getfMemorySizePersistentData
         *
         * @brief
         * 功能: 获取PersistentData段内存大小
         * 
         * @return
         * long: PersistentData段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizePersistentData() {
            return fMemorySizePersistentData;
        }
        
        /**
         * setfMemorySizePersistentData
         *
         * @brief
         * 功能: 设置PersistentData段内存大小
         * 
         * @param[in]  fMemorySizePersistentData: PersistentData段内存大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizePersistentData(long fMemorySizePersistentData) {
            this.fMemorySizePersistentData = fMemorySizePersistentData;
        }
        
        /**
         * getfMemorySizePersistentBss
         *
         * @brief
         * 功能: 获取PersistentBss段内存大小
         * 
         * @return
         * long: PersistentBss段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getfMemorySizePersistentBss() {
            return fMemorySizePersistentBss;
        }
        
        /**
         * setfMemorySizePersistentBss
         *
         * @brief
         * 功能: 设置PersistentBss段内存大小
         * 
         * @param[in]  fMemorySizePersistentBss: PersistentBss段内存大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfMemorySizePersistentBss(long fMemorySizePersistentBss) {
            this.fMemorySizePersistentBss = fMemorySizePersistentBss;
        }
    }
}
