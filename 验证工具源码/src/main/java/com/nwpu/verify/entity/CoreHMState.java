/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreHMState.java
 * 
 * 描述    : 核心健康监控状态实体类，对应XML中的CoreHmState元素
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

public class CoreHMState {
    
    /* 成员变量 */
    private String description;
    private int stateIdentifier;
    
    /**
     * CoreHMState
     *
     * @brief
     * 功能: 默认构造函数
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreHMState() {
    }
    
    /**
     * CoreHMState
     *
     * @brief
     * 功能: 带参构造函数，初始化描述和状态标识符
     * 
     * @param[in]  description: Description属性
     * @param[in]  stateIdentifier: StateIdentifier属性
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreHMState(String description, int stateIdentifier) {
        this.description = description;
        this.stateIdentifier = stateIdentifier;
    }
    
    /**
     * getDescription
     *
     * @brief
     * 功能: 获取描述信息
     * 
     * @return
     * String: 描述信息
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * setDescription
     *
     * @brief
     * 功能: 设置描述信息
     * 
     * @param[in]  description: 描述信息
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * getStateIdentifier
     *
     * @brief
     * 功能: 获取状态标识符
     * 
     * @return
     * int: 状态标识符
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getStateIdentifier() {
        return stateIdentifier;
    }
    
    /**
     * setStateIdentifier
     *
     * @brief
     * 功能: 设置状态标识符
     * 
     * @param[in]  stateIdentifier: 状态标识符
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setStateIdentifier(int stateIdentifier) {
        this.stateIdentifier = stateIdentifier;
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
        return "CoreHMState{" +
                "description='" + description + '\'' +
                ", stateIdentifier=" + stateIdentifier +
                '}';
    }
}
