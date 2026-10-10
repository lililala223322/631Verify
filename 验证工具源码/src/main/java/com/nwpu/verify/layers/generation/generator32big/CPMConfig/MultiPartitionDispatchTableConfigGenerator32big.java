/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MultiPartitionDispatchTableConfigGenerator32big.java
 * 
 * 描述    : 生成MultiPartitionDispatchTableConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMConfig;

import com.nwpu.verify.entity.HealthMonitor;
import com.nwpu.verify.entity.MultiPartitionHMTable;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 多分区分配表配置存储结构生成器 (32位大端)根据 MultiPartitionHMTable 中的 ErrorAction 生成多分区分配表配置存储结构。
 */
public class MultiPartitionDispatchTableConfigGenerator32big {

    /**
     * generate
     *
     * @brief
     * 功能: 生成多分区分配表配置存储结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 从XML解析层传入的顶层Module对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映。。
     * @return 生成的多分区分配表配置存储结构二进制字节数组
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            HealthMonitor healthMonitor = module.getfHealthMonitor();
            if (healthMonitor == null) {
                return baos.toByteArray(); // 如果没有健康监控配置，返回空数组
            }
            
            Map<String, MultiPartitionHMTable> multiPartitionHMTables = healthMonitor.getMultiPartitionHMTables();
            if (multiPartitionHMTables == null || multiPartitionHMTables.isEmpty()) {
                return baos.toByteArray(); // 如果没有多分区健康监控表，返回空数组
            }
            
            int dispatchTableIndex = 0;
            
            // 遍历所。MultiPartitionHMTable
            for (MultiPartitionHMTable multiPartitionHMTable : multiPartitionHMTables.values()) {
                if (multiPartitionHMTable.getErrorActions() == null) {
                    continue;
                }
                
                // 遍历每个 ErrorAction，为每个 ErrorAction 生成一个多分区分配表配置存储结。
                for (MultiPartitionHMTable.ErrorAction errorAction : multiPartitionHMTable.getErrorActions()) {
                    // 字段1: 系统状态(4字节)
                    // 固定值正整数"0"
                    int systemState = 0;
                    byte[] systemStateBytes = BinaryUtils.intToBytesBigEndian(systemState);
                    baos.write(systemStateBytes);
                    fieldMap.put("MultiPartitionDispatchTable[" + dispatchTableIndex + "].SystemState", systemStateBytes);
                    
                    // 字段2: 分派级别 (4字节)
                    // 根据 ErrorLevel 属性值映射：MODULE -> 3, PARTITION -> 1
                    int dispatchLevel = mapErrorLevel(getErrorLevel(errorAction));
                    byte[] dispatchLevelBytes = BinaryUtils.intToBytesBigEndian(dispatchLevel);
                    baos.write(dispatchLevelBytes);
                    fieldMap.put("MultiPartitionDispatchTable[" + dispatchTableIndex + "].DispatchLevel", dispatchLevelBytes);
                    
                    // 字段3: 故障代码 (4字节)
                    // ErrorAction ErrorIdentifierRef 属性
                    int errorCode = errorAction.getErrorIdentifierRef();
                    byte[] errorCodeBytes = BinaryUtils.intToBytesBigEndian(errorCode);
                    baos.write(errorCodeBytes);
                    fieldMap.put("MultiPartitionDispatchTable[" + dispatchTableIndex + "].ErrorCode", errorCodeBytes);
                    
                    
                    // 字段4: 处理动作名称 (64字节字符数组)
                    // 根据 ModuleRecoveryAction 属性值进行映射
                    String actionName = mapModuleRecoveryAction(errorAction.getModuleRecoveryActionAsString());
                    byte[] actionNameBytes = BinaryUtils.fixedLengthAscii(actionName, 64, '\0');
                    baos.write(actionNameBytes);
                    fieldMap.put("MultiPartitionDispatchTable[" + dispatchTableIndex + "].ActionName", actionNameBytes);
                    
                    dispatchTableIndex++;
                }
            }
            
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算多分区分配表配置存储结构所需的总字节数。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 从XML解析层传入的顶层Module对象
     * @return 总字节数
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        HealthMonitor healthMonitor = module.getfHealthMonitor();
        if (healthMonitor == null) {
            return 0;
        }
        
        Map<String, MultiPartitionHMTable> multiPartitionHMTables = healthMonitor.getMultiPartitionHMTables();
        if (multiPartitionHMTables == null || multiPartitionHMTables.isEmpty()) {
            return 0;
        }
        
        int totalErrorActions = 0;
        
        // 统计所有 MultiPartitionHMTable 中的 ErrorAction 总数
        for (MultiPartitionHMTable multiPartitionHMTable : multiPartitionHMTables.values()) {
            if (multiPartitionHMTable.getErrorActions() != null) {
                totalErrorActions += multiPartitionHMTable.getErrorActions().size();
            }
        }
        
        // 每个多分区分配表配置存储结构占用
        // 系统状。4字节) + 分派级别(4字节) + 故障代码(4字节) + 处理动作名称(64字节) = 76字节
        return totalErrorActions * 76;
    }
    
    /**
     * mapErrorLevel
     *
     * @brief
     * 功能: 根据 ErrorLevel 属性值映射为相应的分派级别数。
     *
     * @param[in]  errorLevel: ErrorLevel 属性值
     * @return 映射后的分派级别数。。
     * @scope internal
     * @errno:
     * N/A
     */
    private static int mapErrorLevel(String errorLevel) {
        if (errorLevel == null || errorLevel.trim().isEmpty()) {
            return 1; // 默认 PARTITION 级别
        }
        
        switch (errorLevel.trim().toUpperCase()) {
            case "MODULE":
                return 3;
            case "PARTITION":
                return 1;
            default:
                return 1; // 默认 PARTITION 级别
        }
    }
    
    /**
     * mapModuleRecoveryAction
     *
     * @brief
     * 功能: 根据 ModuleRecoveryAction 属性值映射为相应的处理动作名称。
     *
     * @param[in]  moduleRecoveryAction: ModuleRecoveryAction 属性值
     * @return 映射后的处理动作名称
     * @scope internal
     * @errno:
     * N/A
     */
    private static String mapModuleRecoveryAction(String moduleRecoveryAction) {
        if (moduleRecoveryAction == null || moduleRecoveryAction.trim().isEmpty()) {
            return "NULL";
        }
        
        switch (moduleRecoveryAction.trim().toUpperCase()) {
            case "IGNORE":
                return "NULL";
            case "SHUTDOWN":
                return "ACoreOs_hm_module_stop_default_handler";
            case "RESET":
                return "ACoreOs_hm_module_reboot_default_handler";
            default:
                // 当属性值不是上述几种值时，取值为 ModuleRecoveryAction 属性原值
                return moduleRecoveryAction;
        }
    }
    
    /**
     * getErrorLevel
     *
     * @brief
     * 功能: 获取 ErrorAction ErrorLevel 属性。
     *
     * @param[in]  errorAction: ErrorAction 对象
     * @return ErrorLevel 属性
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getErrorLevel(MultiPartitionHMTable.ErrorAction errorAction) {
        return errorAction.getErrorLevelAsString();
    }
}

