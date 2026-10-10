/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: LogBook.java
 * 
 * 描述    : 日志记事本配置实体类
 *           对应XML中的 LogBookList/LogBook 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public class LogBook {

    /* 成员变量 */
    private char[] fLogbookName = new char[32]; // 日志记事本名，32字节
    private long fMaxMessageSize; // 最大消息长度，unsigned int, 4字节
    private long fMaxNBLoggedMessages; // 最大消息数，unsigned int, 4字节
    private long fMaxNBInProgressMessages; // RAM缓冲的最大消息数，unsigned int, 4字节
    private long fSerialNumber; // 日志记事本编号，unsigned int, 4字节
    private String partitionNameRef; // 所属分区的引用名称

    /**
     * LogBook
     *
     * @brief
     * 功能: 默认构造函数
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public LogBook() {
        /* 默认构造函数 */
    }

    /**
     * getfLogbookName
     *
     * @brief
     * 功能: 获取日志记事本名字符数组
     * 
     * @return
     * char[]: 日志记事本名字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getfLogbookName() {
        return fLogbookName;
    }

    /**
     * setfLogbookName
     *
     * @brief
     * 功能: 设置日志记事本名字符数组
     * 
     * @param[in]  fLogbookName: 日志记事本名字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfLogbookName(char[] fLogbookName) {
        this.fLogbookName = fLogbookName;
    }

    /**
     * getfMaxMessageSize
     *
     * @brief
     * 功能: 获取最大消息长度
     * 
     * @return
     * long: 最大消息长度
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
     * 功能: 设置最大消息长度
     * 
     * @param[in]  fMaxMessageSize: 最大消息长度
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
     * getfMaxNBLoggedMessages
     *
     * @brief
     * 功能: 获取最大消息数
     * 
     * @return
     * long: 最大消息数
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxNBLoggedMessages() {
        return fMaxNBLoggedMessages;
    }

    /**
     * setfMaxNBLoggedMessages
     *
     * @brief
     * 功能: 设置最大消息数
     * 
     * @param[in]  fMaxNBLoggedMessages: 最大消息数
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxNBLoggedMessages(long fMaxNBLoggedMessages) {
        this.fMaxNBLoggedMessages = fMaxNBLoggedMessages;
    }

    /**
     * getfMaxNBInProgressMessages
     *
     * @brief
     * 功能: 获取RAM缓冲的最大消息数
     * 
     * @return
     * long: RAM缓冲的最大消息数
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfMaxNBInProgressMessages() {
        return fMaxNBInProgressMessages;
    }

    /**
     * setfMaxNBInProgressMessages
     *
     * @brief
     * 功能: 设置RAM缓冲的最大消息数
     * 
     * @param[in]  fMaxNBInProgressMessages: RAM缓冲的最大消息数
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxNBInProgressMessages(long fMaxNBInProgressMessages) {
        this.fMaxNBInProgressMessages = fMaxNBInProgressMessages;
    }

    /**
     * getfSerialNumber
     *
     * @brief
     * 功能: 获取日志记事本编号
     * 
     * @return
     * long: 日志记事本编号
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public long getfSerialNumber() {
        return fSerialNumber;
    }

    /**
     * setfSerialNumber
     *
     * @brief
     * 功能: 设置日志记事本编号
     * 
     * @param[in]  fSerialNumber: 日志记事本编号
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfSerialNumber(long fSerialNumber) {
        this.fSerialNumber = fSerialNumber;
    }

    /**
     * getPartitionNameRef
     *
     * @brief
     * 功能: 获取所属分区的引用名称
     * 
     * @return
     * String: 所属分区的引用名称
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getPartitionNameRef() {
        return partitionNameRef;
    }

    /**
     * setPartitionNameRef
     *
     * @brief
     * 功能: 设置所属分区的引用名称
     * 
     * @param[in]  partitionNameRef: 所属分区的引用名称
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setPartitionNameRef(String partitionNameRef) {
        this.partitionNameRef = partitionNameRef;
    }
}