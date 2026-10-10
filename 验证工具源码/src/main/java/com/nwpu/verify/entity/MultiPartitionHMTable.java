/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MultiPartitionHMTable.java
 * 
 * 描述    : 多分区健康监控表配置实体类
 *           对应XML中的 <MultiPartitionHMTable> 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.List;

public class MultiPartitionHMTable {

    /* 成员变量 */
    private char[] name = new char[32];
    private char[] description = new char[64];
    private int stateIdentifier;
    private List<ErrorAction> errorActions;

    /**
     * MultiPartitionHMTable
     *
     * @brief
     * 功能: 默认构造函数，初始化错误动作列表并将字符数组填充为0
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public MultiPartitionHMTable() {
        this.errorActions = new ArrayList<>();
        /* 初始化字符数组为0 */
        for (int i = 0; i < name.length; i++) {
            name[i] = 0;
        }
        for (int i = 0; i < description.length; i++) {
            description[i] = 0;
        }
    }

    /**
     * getName
     *
     * @brief
     * 功能: 获取名称字符数组
     * 
     * @return
     * char[]: 名称字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getName() {
        return name;
    }

    /**
     * setName
     *
     * @brief
     * 功能: 以字符数组设置名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  name: 名称字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setName(char[] name) {
        int length = Math.min(name.length, this.name.length);
        for (int i = 0; i < length; i++) {
            this.name[i] = name[i];
        }
        for (int i = length; i < this.name.length; i++) {
            this.name[i] = 0;
        }
    }

    /**
     * setName
     *
     * @brief
     * 功能: 以字符串设置名称
     * 
     * @param[in]  name: 名称字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setName(String name) {
        setName(name.toCharArray());
    }

    /**
     * getNameAsString
     *
     * @brief
     * 功能: 将名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getNameAsString() {
        int endIndex = 0;
        while (endIndex < name.length && name[endIndex] != 0) {
            endIndex++;
        }
        return new String(name, 0, endIndex);
    }

    /**
     * getDescription
     *
     * @brief
     * 功能: 获取描述字符数组
     * 
     * @return
     * char[]: 描述字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getDescription() {
        return description;
    }

    /**
     * setDescription
     *
     * @brief
     * 功能: 以字符数组设置描述，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  description: 描述字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setDescription(char[] description) {
        int length = Math.min(description.length, this.description.length);
        for (int i = 0; i < length; i++) {
            this.description[i] = description[i];
        }
        for (int i = length; i < this.description.length; i++) {
            this.description[i] = 0;
        }
    }

    /**
     * setDescription
     *
     * @brief
     * 功能: 以字符串设置描述
     * 
     * @param[in]  description: 描述字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setDescription(String description) {
        setDescription(description.toCharArray());
    }

    /**
     * getDescriptionAsString
     *
     * @brief
     * 功能: 将描述字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 描述字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getDescriptionAsString() {
        int endIndex = 0;
        while (endIndex < description.length && description[endIndex] != 0) {
            endIndex++;
        }
        return new String(description, 0, endIndex);
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
     * getErrorActions
     *
     * @brief
     * 功能: 获取错误动作列表
     * 
     * @return
     * List<ErrorAction>: 错误动作列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<ErrorAction> getErrorActions() {
        return errorActions;
    }

    /**
     * setErrorActions
     *
     * @brief
     * 功能: 设置错误动作列表
     * 
     * @param[in]  errorActions: 错误动作列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setErrorActions(List<ErrorAction> errorActions) {
        this.errorActions = errorActions;
    }

    /**
     * addErrorAction
     *
     * @brief
     * 功能: 向错误动作列表添加一项
     * 
     * @param[in]  errorAction: 错误动作配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addErrorAction(ErrorAction errorAction) {
        this.errorActions.add(errorAction);
    }

    /**
     * ErrorAction
     *
     * @brief
     * 功能: 内部类：ErrorAction，错误动作配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class ErrorAction {
        private char[] moduleRecoveryAction = new char[32];
        private char[] errorLevel = new char[16]; // ErrorLevel 字段
        private int errorIdentifierRef;

        /**
         * ErrorAction
         *
         * @brief
         * 功能: 默认构造函数，将字符数组填充为0
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public ErrorAction() {
            /* 初始化字符数组为0 */
            for (int i = 0; i < moduleRecoveryAction.length; i++) {
                moduleRecoveryAction[i] = 0;
            }
            for (int i = 0; i < errorLevel.length; i++) {
                errorLevel[i] = 0;
            }
        }

        /**
         * getModuleRecoveryAction
         *
         * @brief
         * 功能: 获取模块恢复动作字符数组
         * 
         * @return
         * char[]: 模块恢复动作字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getModuleRecoveryAction() {
            return moduleRecoveryAction;
        }

        /**
         * setModuleRecoveryAction
         *
         * @brief
         * 功能: 以字符数组设置模块恢复动作，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  moduleRecoveryAction: 模块恢复动作字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setModuleRecoveryAction(char[] moduleRecoveryAction) {
            int length = Math.min(moduleRecoveryAction.length, this.moduleRecoveryAction.length);
            for (int i = 0; i < length; i++) {
                this.moduleRecoveryAction[i] = moduleRecoveryAction[i];
            }
            for (int i = length; i < this.moduleRecoveryAction.length; i++) {
                this.moduleRecoveryAction[i] = 0;
            }
        }

        /**
         * setModuleRecoveryAction
         *
         * @brief
         * 功能: 以字符串设置模块恢复动作
         * 
         * @param[in]  moduleRecoveryAction: 模块恢复动作字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setModuleRecoveryAction(String moduleRecoveryAction) {
            setModuleRecoveryAction(moduleRecoveryAction.toCharArray());
        }

        /**
         * getModuleRecoveryActionAsString
         *
         * @brief
         * 功能: 将模块恢复动作字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 模块恢复动作字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getModuleRecoveryActionAsString() {
            int endIndex = 0;
            while (endIndex < moduleRecoveryAction.length && moduleRecoveryAction[endIndex] != 0) {
                endIndex++;
            }
            return new String(moduleRecoveryAction, 0, endIndex);
        }

        /**
         * getErrorLevel
         *
         * @brief
         * 功能: 获取错误级别字符数组
         * 
         * @return
         * char[]: 错误级别字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getErrorLevel() {
            return errorLevel;
        }

        /**
         * setErrorLevel
         *
         * @brief
         * 功能: 以字符数组设置错误级别，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  errorLevel: 错误级别字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setErrorLevel(char[] errorLevel) {
            int length = Math.min(errorLevel.length, this.errorLevel.length);
            for (int i = 0; i < length; i++) {
                this.errorLevel[i] = errorLevel[i];
            }
            for (int i = length; i < this.errorLevel.length; i++) {
                this.errorLevel[i] = 0;
            }
        }

        /**
         * setErrorLevel
         *
         * @brief
         * 功能: 以字符串设置错误级别
         * 
         * @param[in]  errorLevel: 错误级别字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setErrorLevel(String errorLevel) {
            setErrorLevel(errorLevel.toCharArray());
        }

        /**
         * getErrorLevelAsString
         *
         * @brief
         * 功能: 将错误级别字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 错误级别字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getErrorLevelAsString() {
            int endIndex = 0;
            while (endIndex < errorLevel.length && errorLevel[endIndex] != 0) {
                endIndex++;
            }
            return new String(errorLevel, 0, endIndex);
        }

        /**
         * getErrorIdentifierRef
         *
         * @brief
         * 功能: 获取错误标识符引用
         * 
         * @return
         * int: 错误标识符引用
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getErrorIdentifierRef() {
            return errorIdentifierRef;
        }

        /**
         * setErrorIdentifierRef
         *
         * @brief
         * 功能: 设置错误标识符引用
         * 
         * @param[in]  errorIdentifierRef: 错误标识符引用
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setErrorIdentifierRef(int errorIdentifierRef) {
            this.errorIdentifierRef = errorIdentifierRef;
        }
    }
}
