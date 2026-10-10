/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionDispatchTableGenerator32small.java
 * 
 * 描述    : 生成PartitionDispatchTable相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

import com.nwpu.verify.entity.PartitionHMTable;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 分区分配表配置二进制生成器 (32位大端)。
 */
public class PartitionDispatchTableGenerator32small {

    // 固定值常量
    private static final int SYSTEM_STATUS = 0; // 系统状态：固定值正整数"0"
    private static final int ACTION_NAME_SIZE = 64; // 处理动作名称长度(4字节)

    // 处理动作名称映射
    private static final String ACTION_IGNORE = "NULL";
    private static final String ACTION_IDLE = "ACoreOs_hm_event_show_default_handler";
    private static final String ACTION_WARM_RESTART = "ACoreOs_hm_part_warm_restart_default_handler";
    private static final String ACTION_COLD_RESTART = "ACoreOs_hm_part_cold_restart_default_handler";

    /**
     * generateSinglePartitionDispatchTable
     *
     * @brief
     * 功能: 生成单个分区分配表配置存储结构的二进制内容。
     *
     * @param[in]  errorLevel: 错误级别 (PARTITION | PROCESS)
     * @param[in]  errorIdentifierRef: 故障代码
     * @param[in]  partitionRecoveryAction: 分区恢复动作
     * @return 单个分区分配表配置存储结构的二进制内容
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateSinglePartitionDispatchTable(String errorLevel, int errorIdentifierRef, 
                                                               String partitionRecoveryAction) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 字段 1: 系统状态(4字节) - 固定值正整数"0"
        baos.write(BinaryUtils.intToBytesLittleEndian(SYSTEM_STATUS));

        // 字段 2: 分派级别 (4字节)
        int dispatchLevel = getDispatchLevel(errorLevel);
        baos.write(BinaryUtils.intToBytesLittleEndian(dispatchLevel));

        // 字段 3: 故障代码 (4字节) - ErrorIdentifierRef的属性值
        baos.write(BinaryUtils.intToBytesLittleEndian(errorIdentifierRef));

        // 字段 4: 处理动作名称 (64字节) - 字符数组，不足以0字节填充
        String actionName = getActionName(partitionRecoveryAction);
        byte[] actionNameBytes = BinaryUtils.fixedLengthAscii(actionName, ACTION_NAME_SIZE, '\0');
        baos.write(actionNameBytes);

        return baos.toByteArray();
    }

    /**
     * getDispatchLevel
     *
     * @brief
     * 功能: 根据ErrorLevel获取分派级别。
     *
     * @param[in]  errorLevel: 错误级别
     * @return 分派级别
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getDispatchLevel(String errorLevel) {
        if ("PARTITION".equalsIgnoreCase(errorLevel)) {
            return 1; // 正整数"1"
        } else if ("PROCESS".equalsIgnoreCase(errorLevel)) {
            return 0; // 正整数"0"
        } else {
            // 默认值，如果ErrorLevel不是预期值
            return 0;
        }
    }

    /**
     * getActionName
     *
     * @brief
     * 功能: 根据PartitionRecoveryAction获取处理动作名称。
     *
     * @param[in]  partitionRecoveryAction: 分区恢复动作
     * @return 处理动作名称
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getActionName(String partitionRecoveryAction) {
        if (partitionRecoveryAction == null || partitionRecoveryAction.isEmpty()) {
            return ACTION_IGNORE;
        }

        switch (partitionRecoveryAction.toUpperCase()) {
            case "IGNORE":
                return ACTION_IGNORE;
            case "IDLE":
                return ACTION_IDLE;
            case "WARM_RESTART":
                return ACTION_WARM_RESTART;
            case "COLD_RESTART":
                return ACTION_COLD_RESTART;
            default:
                // 当属性值不是上述几种值时，取值为PartitionRecoveryAction属性原值
                return partitionRecoveryAction;
        }
    }

    /**
     * generatePartitionDispatchTables
     *
     * @brief
     * 功能: 生成所有分区分配表配置存储结构的二进制数据。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 模块对象
     * @param[in]  fieldMap: 字段映射，用于记录生成的二进制内容
     * @return 所有分区分配表配置存储结构的二进制数据
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generatePartitionDispatchTables(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap) throws IOException {
        if (module.getfHealthMonitor() == null || 
            module.getfHealthMonitor().getPartitionHMTables() == null ||
            module.getfHealthMonitor().getPartitionHMTables().isEmpty()) {
            return new byte[0]; // 如果没有分区健康监控表，返回空数组
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, PartitionHMTable> partitionHMTables = module.getfHealthMonitor().getPartitionHMTables();

        // 按照PartitionHMTable在PartitionHMTableList下的先后顺序依次生成
        for (Map.Entry<String, PartitionHMTable> tableEntry : partitionHMTables.entrySet()) {
            String tableName = tableEntry.getKey();
            PartitionHMTable partitionHMTable = tableEntry.getValue();

            if (partitionHMTable.getErrorActions() != null) {
                int actionIndex = 0;
                // 按照ErrorAction在PartitionHMTable下的顺序依次存储
                for (PartitionHMTable.ErrorAction errorAction : partitionHMTable.getErrorActions()) {
                    String errorLevel = errorAction.getErrorLevelAsString();
                    int errorIdentifierRef = errorAction.getErrorIdentifierRef();
                    String partitionRecoveryAction = errorAction.getPartitionRecoveryActionAsString();

                    // 生成单个分区分配表配置存储结构
                    byte[] singleTableBytes = generateSinglePartitionDispatchTable(
                        errorLevel, errorIdentifierRef, partitionRecoveryAction);
                    
                    baos.write(singleTableBytes);

                    // 记录到fieldMap中，便于后续对比
                    String fieldKey = String.format("PartitionDispatchTable_%s_%d", tableName, actionIndex);
                    fieldMap.put(fieldKey, singleTableBytes);

                    actionIndex++;
                }
            }
        }


        // 对齐由OsConfigGenerator在三类分派表整体之后统一处理，避免在分区分派表末尾重复填充。
        return baos.toByteArray();
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算分区分配表配置存储结构的总大小（包含8字节对齐填充）。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 模块对象
     * @return 配置大小（字节），已8字节对齐
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        if (module.getfHealthMonitor() == null || 
            module.getfHealthMonitor().getPartitionHMTables() == null ||
            module.getfHealthMonitor().getPartitionHMTables().isEmpty()) {
            return 0;
        }

        Map<String, PartitionHMTable> partitionHMTables = module.getfHealthMonitor().getPartitionHMTables();
        
        // 每个分区分配表配置存储结构的固定大小
        // 系统状态(4) + 分派级别(4) + 故障代码(4) + 处理动作名称(64) = 76字节
        int singleTableSize = 4 + 4 + 4 + ACTION_NAME_SIZE;
        
        int totalErrorActions = 0;
        
        // 计算所有PartitionHMTable下的ErrorAction总数
        for (PartitionHMTable partitionHMTable : partitionHMTables.values()) {
            if (partitionHMTable.getErrorActions() != null) {
                totalErrorActions += partitionHMTable.getErrorActions().size();
            }
        }

        int baseSize = totalErrorActions * singleTableSize;
        
        // 返回分区分派表原始条目总大小；三类分派表的整体对齐由OsConfigGenerator统一处理。
        return baseSize;
    }

    /**
     * calculateSingleSize
     *
     * @brief
     * 功能: 计算单个分区分配表配置存储结构的大小。
     *
     * @return 单个结构的大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleSize() {
        // 系统状态(4) + 分派级别(4) + 故障代码(4) + 处理动作名称(64) = 76字节
        return 4 + 4 + 4 + ACTION_NAME_SIZE;
    }

}
