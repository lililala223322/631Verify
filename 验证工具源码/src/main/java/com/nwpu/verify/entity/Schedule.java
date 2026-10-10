/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Schedule.java
 * 
 * 描述    : 调度配置实体类
 *           对应XML中的 /Module/Schedules/Schedule 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.List;

public class Schedule {

    /* 成员变量 */
    private int scheduleIdentifier;
    private boolean initialModuleSchedule;
    private int coreNumber;
    private char[] scheduleName = new char[32];
    private List<PartitionWindow> partitionWindows;

    /**
     * Schedule
     *
     * @brief
     * 功能: 默认构造函数，初始化分区窗口列表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Schedule() {
        this.partitionWindows = new ArrayList<>();
    }

    /**
     * getScheduleIdentifier
     *
     * @brief
     * 功能: 获取调度标识符
     * 
     * @return
     * int: 调度标识符
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getScheduleIdentifier() {
        return scheduleIdentifier;
    }

    /**
     * setScheduleIdentifier
     *
     * @brief
     * 功能: 设置调度标识符
     * 
     * @param[in]  scheduleIdentifier: 调度标识符
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setScheduleIdentifier(int scheduleIdentifier) {
        this.scheduleIdentifier = scheduleIdentifier;
    }

    /**
     * isInitialModuleSchedule
     *
     * @brief
     * 功能: 获取是否为初始模块调度
     * 
     * @return
     * boolean: 是否为初始模块调度
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public boolean isInitialModuleSchedule() {
        return initialModuleSchedule;
    }

    /**
     * setInitialModuleSchedule
     *
     * @brief
     * 功能: 设置是否为初始模块调度
     * 
     * @param[in]  initialModuleSchedule: 是否为初始模块调度
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setInitialModuleSchedule(boolean initialModuleSchedule) {
        this.initialModuleSchedule = initialModuleSchedule;
    }

    /**
     * getCoreNumber
     *
     * @brief
     * 功能: 获取核心编号
     * 
     * @return
     * int: 核心编号
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getCoreNumber() {
        return coreNumber;
    }

    /**
     * setCoreNumber
     *
     * @brief
     * 功能: 设置核心编号
     * 
     * @param[in]  coreNumber: 核心编号
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setCoreNumber(int coreNumber) {
        this.coreNumber = coreNumber;
    }

    /**
     * getScheduleName
     *
     * @brief
     * 功能: 获取调度名称字符数组
     * 
     * @return
     * char[]: 调度名称字符数组
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public char[] getScheduleName() {
        return scheduleName;
    }

    /**
     * setScheduleName
     *
     * @brief
     * 功能: 以字符数组设置调度名称，超出长度部分截断，不足部分以0填充
     * 
     * @param[in]  scheduleName: 调度名称字符数组
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setScheduleName(char[] scheduleName) {
        int length = Math.min(scheduleName.length, this.scheduleName.length);
        for (int i = 0; i < length; i++) {
            this.scheduleName[i] = scheduleName[i];
        }
        for (int i = length; i < this.scheduleName.length; i++) {
            this.scheduleName[i] = 0;
        }
    }

    /**
     * setScheduleName
     *
     * @brief
     * 功能: 以字符串设置调度名称
     * 
     * @param[in]  scheduleName: 调度名称字符串
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setScheduleName(String scheduleName) {
        setScheduleName(scheduleName.toCharArray());
    }

    /**
     * getPartitionWindows
     *
     * @brief
     * 功能: 获取分区窗口列表
     * 
     * @return
     * List<PartitionWindow>: 分区窗口列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<PartitionWindow> getPartitionWindows() {
        return partitionWindows;
    }

    /**
     * setPartitionWindows
     *
     * @brief
     * 功能: 设置分区窗口列表
     * 
     * @param[in]  partitionWindows: 分区窗口列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setPartitionWindows(List<PartitionWindow> partitionWindows) {
        this.partitionWindows = partitionWindows;
    }

    /**
     * addPartitionWindow
     *
     * @brief
     * 功能: 向分区窗口列表添加一项
     * 
     * @param[in]  window: 分区窗口配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartitionWindow(PartitionWindow window) {
        this.partitionWindows.add(window);
    }

    /**
     * PartitionWindow
     *
     * @brief
     * 功能: 内部类：PartitionWindow，分区窗口配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class PartitionWindow {
        private long duration;
        private char[] partitionNameRef = new char[32];
        private boolean periodicProcessingStart;
        private long offset;

        /**
         * getDuration
         *
         * @brief
         * 功能: 获取持续时间
         * 
         * @return
         * long: 持续时间
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getDuration() {
            return duration;
        }

        /**
         * setDuration
         *
         * @brief
         * 功能: 设置持续时间
         * 
         * @param[in]  duration: 持续时间
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setDuration(long duration) {
            this.duration = duration;
        }

        /**
         * getPartitionNameRef
         *
         * @brief
         * 功能: 获取分区名称引用字符数组
         * 
         * @return
         * char[]: 分区名称引用字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getPartitionNameRef() {
            return partitionNameRef;
        }

        /**
         * setPartitionNameRef
         *
         * @brief
         * 功能: 以字符数组设置分区名称引用，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  partitionNameRef: 分区名称引用字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPartitionNameRef(char[] partitionNameRef) {
            int length = Math.min(partitionNameRef.length, this.partitionNameRef.length);
            for (int i = 0; i < length; i++) {
                this.partitionNameRef[i] = partitionNameRef[i];
            }
            for (int i = length; i < this.partitionNameRef.length; i++) {
                this.partitionNameRef[i] = 0;
            }
        }

        /**
         * setPartitionNameRef
         *
         * @brief
         * 功能: 以字符串设置分区名称引用
         * 
         * @param[in]  partitionNameRef: 分区名称引用字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPartitionNameRef(String partitionNameRef) {
            setPartitionNameRef(partitionNameRef.toCharArray());
        }

        /**
         * getPartitionNameRefAsString
         *
         * @brief
         * 功能: 将分区名称引用字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 分区名称引用字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getPartitionNameRefAsString() {
            int endIndex = 0;
            while (endIndex < partitionNameRef.length && partitionNameRef[endIndex] != 0) {
                endIndex++;
            }
            return new String(partitionNameRef, 0, endIndex);
        }

        /**
         * isPeriodicProcessingStart
         *
         * @brief
         * 功能: 获取是否为周期性处理起始
         * 
         * @return
         * boolean: 是否为周期性处理起始
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isPeriodicProcessingStart() {
            return periodicProcessingStart;
        }

        /**
         * setPeriodicProcessingStart
         *
         * @brief
         * 功能: 设置是否为周期性处理起始
         * 
         * @param[in]  periodicProcessingStart: 是否为周期性处理起始
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPeriodicProcessingStart(boolean periodicProcessingStart) {
            this.periodicProcessingStart = periodicProcessingStart;
        }

        /**
         * getOffset
         *
         * @brief
         * 功能: 获取偏移量
         * 
         * @return
         * long: 偏移量
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getOffset() {
            return offset;
        }

        /**
         * setOffset
         *
         * @brief
         * 功能: 设置偏移量
         * 
         * @param[in]  offset: 偏移量
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setOffset(long offset) {
            this.offset = offset;
        }
    }
}
