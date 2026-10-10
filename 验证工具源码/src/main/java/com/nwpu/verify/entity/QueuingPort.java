/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: QueuingPort.java
 * 
 * 描述    : 排队端口配置实体类，继承Port
 *           对应XML中的 <QueuingPort> 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public class QueuingPort extends Port {

    /* 成员变量 */
    private String fExtendedPort;
    private int fMaxNbMessage;
    private String fProtocolType;
    
    /**
     * getfExtendedPort
     *
     * @brief
     * 功能: 获取扩展端口标识
     * 
     * @return
     * String: 扩展端口标识
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfExtendedPort() {
        return fExtendedPort;
    }
    
    /**
     * setfExtendedPort
     *
     * @brief
     * 功能: 设置扩展端口标识
     * 
     * @param[in]  fExtendedPort: 扩展端口标识
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfExtendedPort(String fExtendedPort) {
        this.fExtendedPort = fExtendedPort;
    }
    
    /**
     * getfMaxNbMessage
     *
     * @brief
     * 功能: 获取最大消息数量
     * 
     * @return
     * int: 最大消息数量
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getfMaxNbMessage() {
        return fMaxNbMessage;
    }
    
    /**
     * setfMaxNbMessage
     *
     * @brief
     * 功能: 设置最大消息数量
     * 
     * @param[in]  fMaxNbMessage: 最大消息数量
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfMaxNbMessage(int fMaxNbMessage) {
        this.fMaxNbMessage = fMaxNbMessage;
    }
    
    /**
     * getfProtocolType
     *
     * @brief
     * 功能: 获取协议类型
     * 
     * @return
     * String: 协议类型
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfProtocolType() {
        return fProtocolType;
    }
    
    /**
     * setfProtocolType
     *
     * @brief
     * 功能: 设置协议类型
     * 
     * @param[in]  fProtocolType: 协议类型
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfProtocolType(String fProtocolType) {
        this.fProtocolType = fProtocolType;
    }
}