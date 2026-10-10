/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedData.java
 * 
 * 描述    : 共享数据配置实体类
 *           对应XML中的 /Module/SharedData 元素
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SharedData {
    
    /* 成员变量 */
    /* SharedData的Name属性，32字节字符数组 */
    private char[] fName = new char[32];
    
    /* SharedDataDescription子元素 */
    private SharedDataDescription fSharedDataDescription;
    
    /* SharedIODescription子元素 */
    private SharedIODescription fSharedIODescription;
    
    /**
     * SharedData
     *
     * @brief
     * 功能: 默认构造函数，初始化名称数组和共享数据描述对象
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public SharedData() {
        /* 初始化名称数组为0 */
        Arrays.fill(fName, (char) 0);
        this.fSharedDataDescription = new SharedDataDescription();
    }
    
    /**
     * getfName
     *
     * @brief
     * 功能: 获取SharedData的Name属性字符数组
     * 
     * @return
     * char[]: Name属性字符数组
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
     * 功能: 以字符数组设置SharedData的Name属性，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: Name属性字符数组
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
        /* 填充剩余部分为0 */
        for (int i = length; i < this.fName.length; i++) {
            this.fName[i] = 0;
        }
    }
    
    /**
     * setfName
     *
     * @brief
     * 功能: 以字符串设置SharedData的Name属性，按US_ASCII编码写入字符数组
     * 
     * @param[in]  fName: Name属性字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfName(String fName) {
        if (fName == null) {
            /* 如果输入为null，填充全0 */
            for (int i = 0; i < this.fName.length; i++) {
                this.fName[i] = 0;
            }
            return;
        }
        
        byte[] bytes = fName.getBytes(StandardCharsets.US_ASCII);
        int length = Math.min(bytes.length, this.fName.length);
        for (int i = 0; i < length; i++) {
            this.fName[i] = (char) bytes[i];
        }
        /* 填充剩余部分为0 */
        for (int i = length; i < this.fName.length; i++) {
            this.fName[i] = 0;
        }
    }
    
    /**
     * getfNameAsString
     *
     * @brief
     * 功能: 将SharedData的Name属性字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: Name属性字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfNameAsString() {
        /* 找到第一个null字符的位置 */
        int endIndex = 0;
        while (endIndex < fName.length && fName[endIndex] != 0) {
            endIndex++;
        }
        
        byte[] bytes = new byte[endIndex];
        for (int i = 0; i < endIndex; i++) {
            bytes[i] = (byte) fName[i];
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }
    
    /**
     * getfSharedDataDescription
     *
     * @brief
     * 功能: 获取SharedDataDescription子元素配置
     * 
     * @return
     * SharedDataDescription: SharedDataDescription子元素配置
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public SharedDataDescription getfSharedDataDescription() {
        return fSharedDataDescription;
    }
    
    /**
     * setfSharedDataDescription
     *
     * @brief
     * 功能: 设置SharedDataDescription子元素配置
     * 
     * @param[in]  fSharedDataDescription: SharedDataDescription子元素配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedDataDescription(SharedDataDescription fSharedDataDescription) {
        this.fSharedDataDescription = fSharedDataDescription;
    }
    
    /**
     * getfSharedIODescription
     *
     * @brief
     * 功能: 获取SharedIODescription子元素配置
     * 
     * @return
     * SharedIODescription: SharedIODescription子元素配置
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public SharedIODescription getfSharedIODescription() {
        return fSharedIODescription;
    }
    
    /**
     * setfSharedIODescription
     *
     * @brief
     * 功能: 设置SharedIODescription子元素配置
     * 
     * @param[in]  fSharedIODescription: SharedIODescription子元素配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSharedIODescription(SharedIODescription fSharedIODescription) {
        this.fSharedIODescription = fSharedIODescription;
    }
    
    /**
     * SharedDataDescription
     *
     * @brief
     * 功能: 内部类：SharedDataDescription，共享数据描述信息
     *       对应XML中的SharedDataDescription元素
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class SharedDataDescription {
        
        /* CachePolicy属性：缓存策略 */
        private String fCachePolicy;
        
        /* DataType属性：数据类型 */
        private String fDataType;
        
        /* Size属性：大小 */
        private String fSize;
        
        /* VirtualAddress属性：虚拟地址 */
        private String fVirtualAddress;
        
        /* SystemAccess属性：系统访问权限（可选） */
        private String fSystemAccess;
        
        /**
         * SharedDataDescription
         *
         * @brief
         * 功能: 默认构造函数，设置共享数据描述默认值
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public SharedDataDescription() {
            /* 设置默认值 */
            this.fCachePolicy = "DEFAULT";
            this.fDataType = "VIRTUAL";
            this.fSize = "0x00010000";
            this.fVirtualAddress = "0";
            this.fSystemAccess = "READ_WRITE"; // 默认读写权限
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
         * getfDataType
         *
         * @brief
         * 功能: 获取数据类型
         * 
         * @return
         * String: 数据类型
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfDataType() {
            return fDataType;
        }
        
        /**
         * setfDataType
         *
         * @brief
         * 功能: 设置数据类型
         * 
         * @param[in]  fDataType: 数据类型
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfDataType(String fDataType) {
            this.fDataType = fDataType;
        }
        
        /**
         * getfSize
         *
         * @brief
         * 功能: 获取大小属性
         * 
         * @return
         * String: 大小属性
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
         * 功能: 设置大小属性
         * 
         * @param[in]  fSize: 大小属性
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
         * getfVirtualAddress
         *
         * @brief
         * 功能: 获取虚拟地址属性
         * 
         * @return
         * String: 虚拟地址属性
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfVirtualAddress() {
            return fVirtualAddress;
        }
        
        /**
         * setfVirtualAddress
         *
         * @brief
         * 功能: 设置虚拟地址属性
         * 
         * @param[in]  fVirtualAddress: 虚拟地址属性
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfVirtualAddress(String fVirtualAddress) {
            this.fVirtualAddress = fVirtualAddress;
        }
        
        /**
         * getfSystemAccess
         *
         * @brief
         * 功能: 获取系统访问权限
         * 
         * @return
         * String: 系统访问权限
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfSystemAccess() {
            return fSystemAccess;
        }
        
        /**
         * setfSystemAccess
         *
         * @brief
         * 功能: 设置系统访问权限
         * 
         * @param[in]  fSystemAccess: 系统访问权限
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSystemAccess(String fSystemAccess) {
            this.fSystemAccess = fSystemAccess;
        }
        
        /**
         * getSizeAsLong
         *
         * @brief
         * 功能: 解析Size属性值为长整数，支持十六进制和十进制格式
         * 
         * @return
         * long: 解析后的大小值
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSizeAsLong() {
            if (fSize == null || fSize.isEmpty()) {
                return 0L;
            }
            
            try {
                if (fSize.startsWith("0x") || fSize.startsWith("0X")) {
                    /* 十六进制格式 */
                    return Long.parseLong(fSize.substring(2), 16);
                } else {
                    /* 十进制格式 */
                    return Long.parseLong(fSize);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse size '" + fSize + "', returning 0");
                return 0L;
            }
        }
        
        /**
         * getVirtualAddressAsLong
         *
         * @brief
         * 功能: 解析VirtualAddress属性值为长整数，支持十六进制和十进制格式
         * 
         * @return
         * long: 解析后的虚拟地址值
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getVirtualAddressAsLong() {
            if (fVirtualAddress == null || fVirtualAddress.isEmpty()) {
                return 0L;
            }
            
            try {
                if (fVirtualAddress.startsWith("0x") || fVirtualAddress.startsWith("0X")) {
                    /* 十六进制格式 */
                    return Long.parseLong(fVirtualAddress.substring(2), 16);
                } else {
                    /* 十进制格式 */
                    return Long.parseLong(fVirtualAddress);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse virtual address '" + fVirtualAddress + "', returning 0");
                return 0L;
            }
        }
    }
    
    /**
     * SharedIODescription
     *
     * @brief
     * 功能: 内部类：SharedIODescription，共享IO描述信息
     *       对应XML中的SharedIODescription元素
     *       根据规范表17的属性及约束条件：
     *       - CachePolicy: ICD规定的14种缓存策略之一，非必须属性
     *       - SystemAccess: READ_ONLY或READ_WRITE，非必须属性
     *       - VirtualAddress: 十进制或十六进制（Byte），必须是addressSpaceRegionAllocationUnit的非负整数倍，非必须属性
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class SharedIODescription {
        
        /* BaseAddress属性：基地址（扩展属性，用于物理地址映射） */
        private String fBaseAddress;
        
        /* Size属性：大小（扩展属性，用于空间大小计算） */
        private String fSize;
        
        /* VirtualAddress属性：共享IO区域的虚拟地址
         * 其大小必须是addressSpaceRegionAllocationUnit的非负整数倍
         * 非必须属性，若未填写则由CPM_GEN工具根据模块默认值定义 */
        private String fVirtualAddress;
        
        /* CachePolicy属性：共享IO区域的缓存策略
         * 枚举值：ICD规定的14种缓存策略
         * 非必须属性，若未填写则由CPM_GEN工具根据模块默认值定义 */
        private String fCachePolicy;
        
        /* SystemAccess属性：决定CoreOS对该共享IO区域的访问权限
         * 枚举值：READ_ONLY、READ_WRITE
         * 非必须属性，若未填写则由CPM_GEN工具根据模块默认值定义 */
        private String fSystemAccess;
        
        /**
         * SharedIODescription
         *
         * @brief
         * 功能: 默认构造函数，根据规范表17设置默认值
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public SharedIODescription() {
            /* 设置默认值 */
            this.fBaseAddress = "0";
            this.fSize = "0x00010000";
            this.fVirtualAddress = "0";
            this.fCachePolicy = "DEFAULT"; // 未配置时使用模块默认缓存策略0x100
            this.fSystemAccess = "READ_WRITE"; // 默认读写权限
        }
        
        /**
         * getfBaseAddress
         *
         * @brief
         * 功能: 获取基地址属性
         * 
         * @return
         * String: 基地址属性
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfBaseAddress() {
            return fBaseAddress;
        }
        
        /**
         * setfBaseAddress
         *
         * @brief
         * 功能: 设置基地址属性
         * 
         * @param[in]  fBaseAddress: 基地址属性
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfBaseAddress(String fBaseAddress) {
            this.fBaseAddress = fBaseAddress;
        }
        
        /**
         * getfSize
         *
         * @brief
         * 功能: 获取大小属性
         * 
         * @return
         * String: 大小属性
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
         * 功能: 设置大小属性
         * 
         * @param[in]  fSize: 大小属性
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
         * getfVirtualAddress
         *
         * @brief
         * 功能: 获取共享IO区域的虚拟地址
         * 
         * @return
         * String: 虚拟地址（十进制或十六进制字符串）
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfVirtualAddress() {
            return fVirtualAddress;
        }
        
        /**
         * setfVirtualAddress
         *
         * @brief
         * 功能: 设置共享IO区域的虚拟地址
         *       约束：必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit的非负整数倍
         *       若未设置或为null，CPM_GEN工具将使用模块默认值定义
         * 
         * @param[in]  fVirtualAddress: 虚拟地址，支持十进制或十六进制格式（如"0x1000"）
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfVirtualAddress(String fVirtualAddress) {
            this.fVirtualAddress = fVirtualAddress;
        }
        
        /**
         * getfCachePolicy
         *
         * @brief
         * 功能: 获取缓存策略
         * 
         * @return
         * String: 缓存策略（ICD规定的14种缓存策略之一）
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
         * 功能: 设置缓存策略，若未设置或为null，CPM_GEN工具将使用模块默认值
         * 
         * @param[in]  fCachePolicy: 缓存策略，应为ICD规定的缓存策略之一
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
         * getfSystemAccess
         *
         * @brief
         * 功能: 获取系统访问权限
         * 
         * @return
         * String: 访问权限（READ_ONLY或READ_WRITE）
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfSystemAccess() {
            return fSystemAccess;
        }
        
        /**
         * setfSystemAccess
         *
         * @brief
         * 功能: 设置系统访问权限，若未设置或为null，CPM_GEN工具将使用模块默认值
         * 
         * @param[in]  fSystemAccess: 访问权限，应为READ_ONLY或READ_WRITE
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfSystemAccess(String fSystemAccess) {
            this.fSystemAccess = fSystemAccess;
        }
        
        /**
         * getSizeAsLong
         *
         * @brief
         * 功能: 解析Size属性值为长整数，支持十六进制和十进制格式
         * 
         * @return
         * long: 解析后的大小值
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSizeAsLong() {
            if (fSize == null || fSize.isEmpty()) {
                return 0L;
            }
            
            try {
                if (fSize.startsWith("0x") || fSize.startsWith("0X")) {
                    /* 十六进制格式 */
                    return Long.parseLong(fSize.substring(2), 16);
                } else {
                    /* 十进制格式 */
                    return Long.parseLong(fSize);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse size '" + fSize + "', returning 0");
                return 0L;
            }
        }
        
        /**
         * getVirtualAddressAsLong
         *
         * @brief
         * 功能: 解析VirtualAddress属性值为长整数，支持十六进制和十进制格式
         * 
         * @return
         * long: 解析后的虚拟地址值
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getVirtualAddressAsLong() {
            if (fVirtualAddress == null || fVirtualAddress.isEmpty()) {
                return 0L;
            }
            
            try {
                if (fVirtualAddress.startsWith("0x") || fVirtualAddress.startsWith("0X")) {
                    /* 十六进制格式 */
                    return Long.parseLong(fVirtualAddress.substring(2), 16);
                } else {
                    /* 十进制格式 */
                    return Long.parseLong(fVirtualAddress);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse virtual address '" + fVirtualAddress + "', returning 0");
                return 0L;
            }
        }
        
        /**
         * getBaseAddressAsLong
         *
         * @brief
         * 功能: 解析BaseAddress属性值为长整数，支持十六进制和十进制格式
         * 
         * @return
         * long: 解析后的基地址值
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getBaseAddressAsLong() {
            if (fBaseAddress == null || fBaseAddress.isEmpty()) {
                return 0L;
            }
            
            try {
                if (fBaseAddress.startsWith("0x") || fBaseAddress.startsWith("0X")) {
                    /* 十六进制格式 */
                    return Long.parseLong(fBaseAddress.substring(2), 16);
                } else {
                    /* 十进制格式 */
                    return Long.parseLong(fBaseAddress);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse base address '" + fBaseAddress + "', returning 0");
                return 0L;
            }
        }
    }
}
