/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionHMTable.java
 * 
 * 描述    : 分区健康监控表配置实体类
 *           对应XML中的 <PartitionHMTable> 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.List;

public class PartitionHMTable {

    /* 成员变量 */
    private char[] multiPartitionHMTableNameRef = new char[32];
    private int tableIdentifier;
    private char[] tableName = new char[32];
    private List<ErrorAction> errorActions;
    private List<PartitionRef> partitionRefs;

    /**
     * PartitionHMTable
     *
     * @brief
     * 功能: 默认构造函数，初始化错误动作列表、分区引用列表并将字符数组填充为0
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public PartitionHMTable() {
        this.errorActions = new ArrayList<>();
        this.partitionRefs = new ArrayList<>();
        /* 初始化字符数组为0 */
        for (int i = 0; i < multiPartitionHMTableNameRef.length; i++) {
            multiPartitionHMTableNameRef[i] = 0;
        }
        for (int i = 0; i < tableName.length; i++) {
            tableName[i] = 0;
        }
    }

    /**
     * getMultiPartitionHMTableNameRef
     *
     * @brief
     * 功能: 获取多分区健康监控表名称引用字符数组
     * 
     * @return
     * char[]: 多分区健康监控表名称引用字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getMultiPartitionHMTableNameRef() {
        return multiPartitionHMTableNameRef;
    }

    /**
     * setMultiPartitionHMTableNameRef
     *
     * @brief
     * 功能: 以字符数组设置多分区健康监控表名称引用，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  multiPartitionHMTableNameRef: 多分区健康监控表名称引用字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMultiPartitionHMTableNameRef(char[] multiPartitionHMTableNameRef) {
        int length = Math.min(multiPartitionHMTableNameRef.length, this.multiPartitionHMTableNameRef.length);
        for (int i = 0; i < length; i++) {
            this.multiPartitionHMTableNameRef[i] = multiPartitionHMTableNameRef[i];
        }
        for (int i = length; i < this.multiPartitionHMTableNameRef.length; i++) {
            this.multiPartitionHMTableNameRef[i] = 0;
        }
    }

    /**
     * setMultiPartitionHMTableNameRef
     *
     * @brief
     * 功能: 以字符串设置多分区健康监控表名称引用
     * 
     * @param[in]  multiPartitionHMTableNameRef: 多分区健康监控表名称引用字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setMultiPartitionHMTableNameRef(String multiPartitionHMTableNameRef) {
        setMultiPartitionHMTableNameRef(multiPartitionHMTableNameRef.toCharArray());
    }

    /**
     * getMultiPartitionHMTableNameRefAsString
     *
     * @brief
     * 功能: 将多分区健康监控表名称引用字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 多分区健康监控表名称引用字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getMultiPartitionHMTableNameRefAsString() {
        int endIndex = 0;
        while (endIndex < multiPartitionHMTableNameRef.length && multiPartitionHMTableNameRef[endIndex] != 0) {
            endIndex++;
        }
        return new String(multiPartitionHMTableNameRef, 0, endIndex);
    }

    /**
     * getTableIdentifier
     *
     * @brief
     * 功能: 获取表标识符
     * 
     * @return
     * int: 表标识符
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getTableIdentifier() {
        return tableIdentifier;
    }

    /**
     * setTableIdentifier
     *
     * @brief
     * 功能: 设置表标识符
     * 
     * @param[in]  tableIdentifier: 表标识符
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setTableIdentifier(int tableIdentifier) {
        this.tableIdentifier = tableIdentifier;
    }

    /**
     * getTableName
     *
     * @brief
     * 功能: 获取表名称字符数组
     * 
     * @return
     * char[]: 表名称字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getTableName() {
        return tableName;
    }

    /**
     * setTableName
     *
     * @brief
     * 功能: 以字符数组设置表名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  tableName: 表名称字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setTableName(char[] tableName) {
        int length = Math.min(tableName.length, this.tableName.length);
        for (int i = 0; i < length; i++) {
            this.tableName[i] = tableName[i];
        }
        for (int i = length; i < this.tableName.length; i++) {
            this.tableName[i] = 0;
        }
    }

    /**
     * setTableName
     *
     * @brief
     * 功能: 以字符串设置表名称
     * 
     * @param[in]  tableName: 表名称字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setTableName(String tableName) {
        setTableName(tableName.toCharArray());
    }

    /**
     * getTableNameAsString
     *
     * @brief
     * 功能: 将表名称字符数组转换为字符串，遇0终止符截断
     * 
     * @return
     * String: 表名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getTableNameAsString() {
        int endIndex = 0;
        while (endIndex < tableName.length && tableName[endIndex] != 0) {
            endIndex++;
        }
        return new String(tableName, 0, endIndex);
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
     * getPartitionRefs
     *
     * @brief
     * 功能: 获取分区引用列表
     * 
     * @return
     * List<PartitionRef>: 分区引用列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<PartitionRef> getPartitionRefs() {
        return partitionRefs;
    }

    /**
     * setPartitionRefs
     *
     * @brief
     * 功能: 设置分区引用列表
     * 
     * @param[in]  partitionRefs: 分区引用列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setPartitionRefs(List<PartitionRef> partitionRefs) {
        this.partitionRefs = partitionRefs;
    }

    /**
     * addPartitionRef
     *
     * @brief
     * 功能: 向分区引用列表添加一项
     * 
     * @param[in]  partitionRef: 分区引用配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartitionRef(PartitionRef partitionRef) {
        this.partitionRefs.add(partitionRef);
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
        private int errorIdentifierRef;
        private char[] partitionRecoveryAction = new char[32];
        private char[] errorLevel = new char[16];

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
            for (int i = 0; i < partitionRecoveryAction.length; i++) {
                partitionRecoveryAction[i] = 0;
            }
            for (int i = 0; i < errorLevel.length; i++) {
                errorLevel[i] = 0;
            }
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

        /**
         * getPartitionRecoveryAction
         *
         * @brief
         * 功能: 获取分区恢复动作字符数组
         * 
         * @return
         * char[]: 分区恢复动作字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getPartitionRecoveryAction() {
            return partitionRecoveryAction;
        }

        /**
         * setPartitionRecoveryAction
         *
         * @brief
         * 功能: 以字符数组设置分区恢复动作，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  partitionRecoveryAction: 分区恢复动作字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPartitionRecoveryAction(char[] partitionRecoveryAction) {
            int length = Math.min(partitionRecoveryAction.length, this.partitionRecoveryAction.length);
            for (int i = 0; i < length; i++) {
                this.partitionRecoveryAction[i] = partitionRecoveryAction[i];
            }
            for (int i = length; i < this.partitionRecoveryAction.length; i++) {
                this.partitionRecoveryAction[i] = 0;
            }
        }

        /**
         * setPartitionRecoveryAction
         *
         * @brief
         * 功能: 以字符串设置分区恢复动作
         * 
         * @param[in]  partitionRecoveryAction: 分区恢复动作字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPartitionRecoveryAction(String partitionRecoveryAction) {
            setPartitionRecoveryAction(partitionRecoveryAction.toCharArray());
        }

        /**
         * getPartitionRecoveryActionAsString
         *
         * @brief
         * 功能: 将分区恢复动作字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 分区恢复动作字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getPartitionRecoveryActionAsString() {
            int endIndex = 0;
            while (endIndex < partitionRecoveryAction.length && partitionRecoveryAction[endIndex] != 0) {
                endIndex++;
            }
            return new String(partitionRecoveryAction, 0, endIndex);
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
    }

    /**
     * PartitionRef
     *
     * @brief
     * 功能: 内部类：PartitionRef，分区引用配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class PartitionRef {
        private int id;

        /**
         * PartitionRef
         *
         * @brief
         * 功能: 默认构造函数
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public PartitionRef() {
        }

        /**
         * PartitionRef
         *
         * @brief
         * 功能: 带分区ID的构造函数
         * 
         * @param[in]  id: 分区ID
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public PartitionRef(int id) {
            this.id = id;
        }

        /**
         * getId
         *
         * @brief
         * 功能: 获取分区ID
         * 
         * @return
         * int: 分区ID
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public int getId() {
            return id;
        }

        /**
         * setId
         *
         * @brief
         * 功能: 设置分区ID
         * 
         * @param[in]  id: 分区ID
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setId(int id) {
            this.id = id;
        }
    }
}
