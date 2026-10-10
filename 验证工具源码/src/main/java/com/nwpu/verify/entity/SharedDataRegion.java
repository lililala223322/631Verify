/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedDataRegion.java
 * 
 * 描述    : 共享数据区配置实体类
 *           对应XML中的 /Module/Partition/SharedDataRegion 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class SharedDataRegion {

    /* 成员变量 */
    /* 共享数据区名, 32字节, 不足填充\0 */
    private char[] fName = new char[32];
    /* 共享数据区 MMU 属性, unsigned int, 4字节 */
    private long fMmuAttribute;
    /* 共享数据区大小 */
    private long fSize;
    /* 访问权限 */
    private String fAccessRights;
    /* 地址 */
    private long fAddress;

    /**
     * SharedDataRegion
     *
     * @brief
     * 功能: 默认构造函数，将名称字符数组填充为0
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public SharedDataRegion() {
        Arrays.fill(fName, (char) 0);
    }

    /**
     * getfName
     *
     * @brief
     * 功能: 获取共享数据区名字符数组
     * 
     * @return
     * char[]: 共享数据区名字符数组
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
     * 功能: 以字符数组设置共享数据区名，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  fName: 共享数据区名字符数组
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
     * 功能: 以字符串设置共享数据区名，按US_ASCII编码写入字符数组
     * 
     * @param[in]  fName: 共享数据区名字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfName(String fName) {
        if (fName == null) {
            Arrays.fill(this.fName, (char) 0);
            return;
        }
        byte[] bytes = fName.getBytes(StandardCharsets.US_ASCII);
        int length = Math.min(bytes.length, this.fName.length);
        for (int i = 0; i < length; i++) {
            this.fName[i] = (char) bytes[i];
        }
        for (int i = length; i < this.fName.length; i++) {
            this.fName[i] = 0;
        }
    }

    /**
     * getfNameAsString
     *
     * @brief
     * 功能: 将共享数据区名字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 共享数据区名字符串
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
        byte[] bytes = new byte[endIndex];
        for (int i = 0; i < endIndex; i++) {
            bytes[i] = (byte) fName[i];
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * getfMmuAttribute
     *
     * @brief
     * 功能: 获取共享数据区MMU属性
     * 
     * @return
     * long: 共享数据区MMU属性
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMmuAttribute() {
        return fMmuAttribute;
    }

    /**
     * setfMmuAttribute
     *
     * @brief
     * 功能: 设置共享数据区MMU属性
     * 
     * @param[in]  fMmuAttribute: 共享数据区MMU属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMmuAttribute(long fMmuAttribute) {
        this.fMmuAttribute = fMmuAttribute;
    }

    /**
     * getfSize
     *
     * @brief
     * 功能: 获取共享数据区大小
     * 
     * @return
     * long: 共享数据区大小
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
     * 功能: 设置共享数据区大小
     * 
     * @param[in]  fSize: 共享数据区大小
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
     * getfAccessRights
     *
     * @brief
     * 功能: 获取访问权限
     * 
     * @return
     * String: 访问权限
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfAccessRights() {
        return fAccessRights;
    }

    /**
     * setfAccessRights
     *
     * @brief
     * 功能: 设置访问权限
     * 
     * @param[in]  fAccessRights: 访问权限
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfAccessRights(String fAccessRights) {
        this.fAccessRights = fAccessRights;
    }
    
    /**
     * getfAddress
     *
     * @brief
     * 功能: 获取地址
     * 
     * @return
     * long: 地址
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfAddress() {
        return fAddress;
    }

    /**
     * setfAddress
     *
     * @brief
     * 功能: 设置地址
     * 
     * @param[in]  fAddress: 地址
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfAddress(long fAddress) {
        this.fAddress = fAddress;
    }
}