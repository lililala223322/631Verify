/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Port.java
 * 
 * 描述    : 端口配置抽象基类
 *           对应XML中 QueuingPort/SamplingPort 的公共属性
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public abstract class Port {

    /* 成员变量 */
    protected char[] fName = new char[32];
    protected String fDirection;
    /* Application端口未配置Attribute时，规范默认值为LOCAL_PORT */
    protected String fAttribute = "LOCAL_PORT";
    protected long fMaxMessageSize;
    protected String fDriverName;
    
    /**
     * getfName
     *
     * @brief
     * 功能: 获取端口名称字符数组
     * 
     * @return
     * char[]: 端口名称字符数组
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
     * 功能: 以字符数组设置端口名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: 端口名称字符数组
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
     * 功能: 以字符串设置端口名称
     * 
     * @param[in]  fName: 端口名称字符串
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
     * 功能: 将端口名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 端口名称字符串
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
     * getfDirection
     *
     * @brief
     * 功能: 获取端口方向
     * 
     * @return
     * String: 端口方向
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfDirection() {
        return fDirection;
    }
    
    /**
     * setfDirection
     *
     * @brief
     * 功能: 设置端口方向
     * 
     * @param[in]  fDirection: 端口方向
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfDirection(String fDirection) {
        this.fDirection = fDirection;
    }
    
    /**
     * getfAttribute
     *
     * @brief
     * 功能: 获取端口属性
     * 
     * @return
     * String: 端口属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfAttribute() {
        return fAttribute;
    }
    
    /**
     * setfAttribute
     *
     * @brief
     * 功能: 设置端口属性
     * 
     * @param[in]  fAttribute: 端口属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfAttribute(String fAttribute) {
        this.fAttribute = fAttribute;
    }
    
    /**
     * getAttributeAsInt
     *
     * @brief
     * 功能: 根据Attribute字符串值映射为整数值
     *       NULL_PORT -> 0
     *       LOCAL_PORT -> 1
     *       PSEUDO_PORT -> 2
     *       DIRECT_ACCESS_PORT -> 3
     * 
     * @return
     * int: 映射后的整数值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getAttributeAsInt() {
        if (fAttribute == null || fAttribute.isEmpty()) {
            return 0; /* 默认为NULL_PORT */
        }
        
        switch (fAttribute.toUpperCase()) {
            case "NULL_PORT":
                return 0;
            case "LOCAL_PORT":
                return 1;
            case "PSEUDO_PORT":
                return 2;
            case "DIRECT_ACCESS_PORT":
                return 3;
            default:
                /* 尝试直接解析为数字 */
                try {
                    return Integer.parseInt(fAttribute);
                } catch (NumberFormatException e) {
                    System.err.println("警告：未知的端口属性 '" + fAttribute + "'，使用默认值 0");
                    return 0;
                }
        }
    }
    
    /**
     * getfMaxMessageSize
     *
     * @brief
     * 功能: 获取最大消息大小
     * 
     * @return
     * long: 最大消息大小
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxMessageSize() {
        return fMaxMessageSize;
    }
    
    /**
     * setfMaxMessageSize
     *
     * @brief
     * 功能: 设置最大消息大小
     * 
     * @param[in]  fMaxMessageSize: 最大消息大小
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxMessageSize(long fMaxMessageSize) {
        this.fMaxMessageSize = fMaxMessageSize;
    }
    
    /**
     * getfDriverName
     *
     * @brief
     * 功能: 获取驱动名称
     * 
     * @return
     * String: 驱动名称
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfDriverName() {
        return fDriverName;
    }
    
    /**
     * setfDriverName
     *
     * @brief
     * 功能: 设置驱动名称
     * 
     * @param[in]  fDriverName: 驱动名称
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfDriverName(String fDriverName) {
        this.fDriverName = fDriverName;
    }
}
