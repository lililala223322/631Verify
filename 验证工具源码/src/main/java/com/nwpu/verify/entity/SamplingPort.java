/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SamplingPort.java
 * 
 * 描述    : 采样端口配置实体类，继承Port
 *           对应XML中的 <SamplingPort> 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public class SamplingPort extends Port {

    /* 成员变量 */
    private String fRefreshRate;
    
    /**
     * getfRefreshRate
     *
     * @brief
     * 功能: 获取刷新率
     * 
     * @return
     * String: 刷新率
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfRefreshRate() {
        return fRefreshRate;
    }
    
    /**
     * setfRefreshRate
     *
     * @brief
     * 功能: 设置刷新率
     * 
     * @param[in]  fRefreshRate: 刷新率
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfRefreshRate(String fRefreshRate) {
        this.fRefreshRate = fRefreshRate;
    }
}