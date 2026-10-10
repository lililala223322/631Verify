/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreDispatchTableConfigGenerator32small.java
 * 
 * 描述    : 生成CoreDispatchTableConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.HealthMonitor;
import com.nwpu.verify.entity.CoreHMTable;
import com.nwpu.verify.entity.CoreHMState;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @brief
 * 功能: 核心分派表配置存储结构生成器 (32位大端)根据 CoreHMTable 中的 ErrorAction 生成核心分派表配置存储结构。
 */
public class CoreDispatchTableConfigGenerator32small {

    /**
     * generate
     *
     * @brief
     * 功能: 生成核心分派表配置存储结构的二进制内容。
     *
     * @param[in]  module: 从XML解析层传入的顶层Module对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映。。
     * @return 生成的核心分派表配置存储结构二进制字节数。。
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            HealthMonitor healthMonitor = module.getfHealthMonitor();
            if (healthMonitor == null) {
                return baos.toByteArray(); // 如果没有健康监控配置，返回空数组
            }
            
            Map<String, CoreHMTable> coreHMTables = healthMonitor.getCoreHMTables();
            if (coreHMTables == null || coreHMTables.isEmpty()) {
                return baos.toByteArray(); // 如果没有核心健康监控表，返回空数。
            }
            
            int dispatchTableIndex = 0;

            // 遍历所有 CoreHMTable
            for (CoreHMTable coreHMTable : coreHMTables.values()) {
                if (coreHMTable.getErrorActions() == null) {
                    continue;
                }
                
                // 获取该 CoreHMTable 对应的所有 StateIdentifier
                List<Integer> stateIdentifiers = resolveStateIdentifiers(coreHMTable, healthMonitor);
                
                // 为每个 ErrorAction 生成配置结构体
                for (CoreHMTable.ErrorAction errorAction : coreHMTable.getErrorActions()) {
                    for (int stateIdentifier : stateIdentifiers) {
                        generateDispatchTableEntry(baos, fieldMap, dispatchTableIndex++, stateIdentifier, errorAction);
                    }
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
     * 功能: 计算核心分派表配置存储结构所需的总字节数。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 从XML解析层传入的顶层Module对象
     * @return 总字节数
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        HealthMonitor healthMonitor = module.getfHealthMonitor();
        if (healthMonitor == null) {
            return 0;
        }
        
        Map<String, CoreHMTable> coreHMTables = healthMonitor.getCoreHMTables();
        if (coreHMTables == null || coreHMTables.isEmpty()) {
            return 0;
        }
        
        int totalErrorActions = 0;

        // 统计所有 CoreHMTable 中的 ErrorAction 总数，考虑 StateIdentifier 位或解析
        for (CoreHMTable coreHMTable : coreHMTables.values()) {
            if (coreHMTable.getErrorActions() != null) {
                // 获取该 CoreHMTable 对应的所有 StateIdentifier
                List<Integer> stateIdentifiers = resolveStateIdentifiers(coreHMTable, healthMonitor);
                
                // 为每个 ErrorAction 计算生成的结构体数量
                for (CoreHMTable.ErrorAction errorAction : coreHMTable.getErrorActions()) {
                    totalErrorActions += stateIdentifiers.size();
                }
            }
        }
        
        // 每个核心分派表配置存储结构占用：
        // 系统状态 (4字节) + 分派级别 (4字节) + 故障代码 (4字节) + 处理动作名称 (64字节) = 76字节
        return totalErrorActions * 76;
    }
    
    /**
     * calculateSizeForSingleTable
     *
     * @brief
     * 功能: 计算单个CoreHMTable对应的分派表配置存储结构所需的字节数。
     *
     * @param[in]  coreHMTable: CoreHMTable对象
     * @param[in]  healthMonitor: HealthMonitor对象，用于获取CoreHmState列表
     * @return 该CoreHMTable对应的分派表大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSizeForSingleTable(CoreHMTable coreHMTable, HealthMonitor healthMonitor) {
        if (coreHMTable == null || coreHMTable.getErrorActions() == null) {
            return 0;
        }
        
        int totalErrorActions = 0;
        
        // 获取该 CoreHMTable 对应的所有 StateIdentifier
        List<Integer> stateIdentifiers = resolveStateIdentifiers(coreHMTable, healthMonitor);
        
        // 为每个 ErrorAction 计算生成的结构体数量
        for (CoreHMTable.ErrorAction errorAction : coreHMTable.getErrorActions()) {
            totalErrorActions += stateIdentifiers.size();
        }
        
        // 每个核心分派表配置存储结构占用：
        // 系统状态 (4字节) + 分派级别 (4字节) + 故障代码 (4字节) + 处理动作名称 (64字节) = 76字节
        return totalErrorActions * 76;
    }
    
    /**
     * generateDispatchTableEntry
     *
     * @brief
     * 功能: 生成单个核心分派表配置存储结构条目。
     *
     * @param[in]  baos: 字节输出流
     * @param[in]  fieldMap: 字段映射
     * @param[in]  dispatchTableIndex: 分派表索引
     * @param[in]  stateIdentifier: 状态标识符
     * @param[in]  errorAction: 错误动作
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateDispatchTableEntry(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap, 
                                                   int dispatchTableIndex, int stateIdentifier, 
                                                   CoreHMTable.ErrorAction errorAction) throws IOException {
        // 字段1: 系统状态 (4字节)
        byte[] systemStateBytes = BinaryUtils.intToBytesLittleEndian(stateIdentifier);
        baos.write(systemStateBytes);
        fieldMap.put("CoreDispatchTable[" + dispatchTableIndex + "].SystemState", systemStateBytes);
        
        // 字段2: 分派级别 (4字节)
        // 固定值正整数"0"
        int dispatchLevel = 0;
        byte[] dispatchLevelBytes = BinaryUtils.intToBytesLittleEndian(dispatchLevel);
        baos.write(dispatchLevelBytes);
        fieldMap.put("CoreDispatchTable[" + dispatchTableIndex + "].DispatchLevel", dispatchLevelBytes);
        
        // 字段3: 故障代码 (4字节)
        // 来自 ErrorAction 的 ErrorIdentifierRef 属性
        int errorCode = errorAction.getErrorIdentifierRef();
        byte[] errorCodeBytes = BinaryUtils.intToBytesLittleEndian(errorCode);
        baos.write(errorCodeBytes);
        fieldMap.put("CoreDispatchTable[" + dispatchTableIndex + "].ErrorCode", errorCodeBytes);
        
        // 字段4: 处理动作名称 (64字节字符数组)
        // 根据 ModuleRecoveryAction 属性值进行映射
        String actionName = mapModuleRecoveryAction(errorAction.getModuleRecoveryActionAsString());
        byte[] actionNameBytes = BinaryUtils.fixedLengthAscii(actionName, 64, '\0');
        baos.write(actionNameBytes);
        fieldMap.put("CoreDispatchTable[" + dispatchTableIndex + "].ActionName", actionNameBytes);
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
                // 当属性值不是上述值时，取值为 ModuleRecoveryAction 属性原值
                return moduleRecoveryAction;
        }
    }

    /**
     * resolveStateIdentifiers
     *
     * @brief
     * 功能: 先计算所有 CoreHmState 的 StateIdentifier 位或值。如果位或结果覆盖 CoreHMTable 的 StateIdentifier，则将 CoreHMTable 的值按位从高到低拆分；否则返回 CoreHMTable 的原始 StateIdentifier。。
     *
     * @param[in]  coreHMTable: CoreHMTable 对象
     * @param[in]  healthMonitor: HealthMonitor 对象，用于获取 CoreHmState 列表
     * @return StateIdentifier 列表
     * @scope internal
     * @errno:
     * N/A
     */
    private static List<Integer> resolveStateIdentifiers(CoreHMTable coreHMTable, HealthMonitor healthMonitor) {
        List<Integer> stateIdentifiers = new ArrayList<>();
        int tableStateIdentifier = coreHMTable.getStateIdentifier();
        
        // 获取所有 CoreHmState
        Map<String, CoreHMState> coreHMStates = healthMonitor.getCoreHMStates();
        if (coreHMStates == null || coreHMStates.isEmpty()) {
            // 如果没有 CoreHmState，直接使用 CoreHMTable 的 StateIdentifier
            stateIdentifiers.add(tableStateIdentifier);
            return stateIdentifiers;
        }
        
        // 收集所有可能的 CoreHmState StateIdentifier，并按从高到低匹配。
        List<Integer> availableStateIds = new ArrayList<>();
        for (CoreHMState state : coreHMStates.values()) {
            int stateId = state.getStateIdentifier();
            if (stateId != 0) {
                availableStateIds.add(stateId);
            }
        }
        Collections.sort(availableStateIds, Collections.reverseOrder());
        if (availableStateIds.isEmpty()) {
            stateIdentifiers.add(tableStateIdentifier);
            return stateIdentifiers;
        }
        
        int combinedStateIdentifier = 0;
        for (int stateId : availableStateIds) {
            combinedStateIdentifier |= stateId;
        }

        if ((combinedStateIdentifier & tableStateIdentifier) != tableStateIdentifier) {
            stateIdentifiers.add(tableStateIdentifier);
            return stateIdentifiers;
        }

        int remainingStateIdentifier = tableStateIdentifier;
        while (remainingStateIdentifier != 0) {
            int stateBit = Integer.highestOneBit(remainingStateIdentifier);
            stateIdentifiers.add(stateBit);
            remainingStateIdentifier &= ~stateBit;
        }

        return stateIdentifiers;
    }
}
