/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedLibrary.java
 * 
 * 描述    : 共享库配置实体类
 *           对应XML中的 /Module/SharedLibraryRegions/SharedLibrary 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public class SharedLibrary {

    /* 成员变量 */
    private char[] fName = new char[32];
    private boolean fSystemSharedLibrary;
    private long fVirtualAddress;
    private MemorySize fMemorySize;
    
    /**
     * SharedLibrary
     *
     * @brief
     * 功能: 默认构造函数，初始化内存大小配置对象
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public SharedLibrary() {
        this.fMemorySize = new MemorySize();
    }
    
    /**
     * getfName
     *
     * @brief
     * 功能: 获取共享库名称字符数组
     * 
     * @return
     * char[]: 共享库名称字符数组
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
     * 功能: 以字符数组设置共享库名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: 共享库名称字符数组
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
     * 功能: 以字符串设置共享库名称
     * 
     * @param[in]  fName: 共享库名称字符串
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
     * 功能: 将共享库名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 共享库名称字符串
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
     * getfSystemSharedLibrary
     *
     * @brief
     * 功能: 获取是否为系统共享库
     * 
     * @return
     * boolean: 是否为系统共享库
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public boolean getfSystemSharedLibrary() {
        return fSystemSharedLibrary;
    }
    
    /**
     * setfSystemSharedLibrary
     *
     * @brief
     * 功能: 设置是否为系统共享库
     * 
     * @param[in]  fSystemSharedLibrary: 是否为系统共享库
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSystemSharedLibrary(boolean fSystemSharedLibrary) {
        this.fSystemSharedLibrary = fSystemSharedLibrary;
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
     * MemorySize
     *
     * @brief
     * 功能: 内部类：MemorySize，共享库内存大小配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class MemorySize {
        private String fMemorySizeText;
        private String fMemorySizeRoData;
        private String fMemorySizeData;
        private String fMemorySizeBss;
        private String fMemorySizePersistentData;
        private String fMemorySizePersistentBss;
        
        /**
         * getfMemorySizeText
         *
         * @brief
         * 功能: 获取Text段内存大小
         * 
         * @return
         * String: Text段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizeText() {
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
        public void setfMemorySizeText(String fMemorySizeText) {
            this.fMemorySizeText = fMemorySizeText;
        }
        
        /**
         * getfMemorySizeRoData
         *
         * @brief
         * 功能: 获取RoData段内存大小
         * 
         * @return
         * String: RoData段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizeRoData() {
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
        public void setfMemorySizeRoData(String fMemorySizeRoData) {
            this.fMemorySizeRoData = fMemorySizeRoData;
        }
        
        /**
         * getfMemorySizeData
         *
         * @brief
         * 功能: 获取Data段内存大小
         * 
         * @return
         * String: Data段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizeData() {
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
        public void setfMemorySizeData(String fMemorySizeData) {
            this.fMemorySizeData = fMemorySizeData;
        }
        
        /**
         * getfMemorySizeBss
         *
         * @brief
         * 功能: 获取Bss段内存大小
         * 
         * @return
         * String: Bss段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizeBss() {
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
        public void setfMemorySizeBss(String fMemorySizeBss) {
            this.fMemorySizeBss = fMemorySizeBss;
        }
        
        /**
         * getfMemorySizePersistentData
         *
         * @brief
         * 功能: 获取PersistentData段内存大小
         * 
         * @return
         * String: PersistentData段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizePersistentData() {
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
        public void setfMemorySizePersistentData(String fMemorySizePersistentData) {
            this.fMemorySizePersistentData = fMemorySizePersistentData;
        }
        
        /**
         * getfMemorySizePersistentBss
         *
         * @brief
         * 功能: 获取PersistentBss段内存大小
         * 
         * @return
         * String: PersistentBss段内存大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfMemorySizePersistentBss() {
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
        public void setfMemorySizePersistentBss(String fMemorySizePersistentBss) {
            this.fMemorySizePersistentBss = fMemorySizePersistentBss;
        }
        
    }
}
