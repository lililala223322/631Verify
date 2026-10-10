/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreHMTableGenerator64small.java
 * 
 * 描述    : 生成CoreHMTable相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.CoreHMTable;
import com.nwpu.verify.entity.CoreHMState;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @brief
 * 功能: 核心健康监控表配置二进制生成器 (32位大端)。
 */
public class CoreHMTableGenerator64small {

    // 固定值常量
    private static final int CONFIG_TYPE = 1129468933; // 配置类型固定值
    private static final int CORE_ID_ARRAY_SIZE = 255; // 核心ID数组大小
    private static final int PARTITION_COUNT = 1; // 使用该健康监控表的分区数目固定值

    /**
     * generateBinary
     *
     * @brief
     * 功能: 生成单个核心健康监控表的二进制数据。
     *
     * @param[in]  table:                   核心健康监控表配置对象
     * @param[in]  fieldMap:                字段映射
     * @param[in]  dispatchTableStartAddr:  分派表起始地址
     * @param[in]  dispatchTableEntryCount: 分派表条目数
     * @return 二进制字节数组
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateBinary(CoreHMTable table, Map<String, byte[]> fieldMap, long dispatchTableStartAddr,
            int dispatchTableEntryCount) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String prefix = "CoreHMTable_" + table.getNameAsString() + "_";

        // 1. 配置类型 (4字节) - 固定值正整数"1129468933"
        byte[] configTypeBytes = BinaryUtils.intToBytesLittleEndian(CONFIG_TYPE);
        baos.write(configTypeBytes);
        fieldMap.put(prefix + "ConfigType", configTypeBytes);

        // 2. 配置数据大小 (4字节) - 写入当前表的总大小
        int configDataSize = calculateSingleTableSize(); // 使用固定大小计算方法
        byte[] configSizeBytes = BinaryUtils.intToBytesLittleEndian(configDataSize);
        baos.write(configSizeBytes);
        fieldMap.put(prefix + "ConfigDataSize", configSizeBytes);

        // 3. 表名 (32字节) - CoreHMTable元素的Name属性值，不满32字节用'\0'填充
        byte[] nameBytes = BinaryUtils.fixedLengthAscii(table.getNameAsString(), 32, '\0');
        baos.write(nameBytes);
        fieldMap.put(prefix + "Name", nameBytes);

        // 4. 分派表起始地址 (4字节，32位系统) - CPM配置信息可加载文件起始地址+分派表偏移
        byte[] dispatchAddrBytes = BinaryUtils.longTo8BytesLittleEndian(dispatchTableStartAddr);
        baos.write(dispatchAddrBytes);
        fieldMap.put(prefix + "DispatchTableStartAddr", dispatchAddrBytes);

        // 5. 分派表条目数 (4字节) - 本核心健康监控表的核心分派表中结构体个数
        byte[] entryCountBytes = BinaryUtils.intToBytesLittleEndian(dispatchTableEntryCount);
        baos.write(entryCountBytes);
        fieldMap.put(prefix + "DispatchTableEntryCount", entryCountBytes);

        // 6. 使用该健康监控表的核心ID (4*255字节)
        // 优先使用XML中配置的Core/cpuId（已解析到coreRefIds），其余补0
        byte[] coreIdArrayBytes = new byte[4 * CORE_ID_ARRAY_SIZE];
        java.util.List<Long> coreIds = table.getCoreRefIds();
        for (int i = 0; i < CORE_ID_ARRAY_SIZE; i++) {
            int id = 0;
            if (coreIds != null && i < coreIds.size()) {
                id = coreIds.get(i).intValue();
            }
            byte[] coreIdBytes = BinaryUtils.intToBytesLittleEndian(id);
            System.arraycopy(coreIdBytes, 0, coreIdArrayBytes, i * 4, 4);
            baos.write(coreIdBytes);
        }
        fieldMap.put(prefix + "CoreIdArray", coreIdArrayBytes);

        // 7. 使用该健康监控表的分区数目 (4字节) - 固定值正整数"1"
        byte[] partitionCountBytes = BinaryUtils.intToBytesLittleEndian(PARTITION_COUNT);
        baos.write(partitionCountBytes);
        fieldMap.put(prefix + "PartitionCount", partitionCountBytes);

        // 8. 8字节对齐填充
        int currentSize = baos.size();
        int paddingBytes = calculateAlignmentPadding(currentSize);
        if (paddingBytes > 0) {
            // 添加填充字节（全部为0）
            byte[] padding = new byte[paddingBytes];
            baos.write(padding);
            fieldMap.put(prefix + "Padding", padding);
        }

        return baos.toByteArray();
    }

    /**
     * generateCoreHMTableConfigs
     *
     * @brief
     * 功能: 生成所有核心健康监控表配置的二进制内容（用于OsConfigGenerator64small集成）。
     *
     * @param[in]  module:                  模块对象
     * @param[in]  fieldMap:                字段映射
     * @param[in]  cpmStartAddress:         CPM配置信息可加载文件起始地址
     * @param[in]  coreDispatchTableOffset: 核心分派表在CPM文件中的偏移
     * @return 二进制字节数组
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateCoreHMTableConfigs(Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress, long coreDispatchTableOffset) throws IOException {
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getCoreHMTables() == null ||
                module.getfHealthMonitor().getCoreHMTables().isEmpty()) {
            return new byte[0]; // 如果没有核心健康监控表，返回空数组
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, CoreHMTable> coreHMTables = module.getfHealthMonitor().getCoreHMTables();

        // 用于累加计算每个表的起始地址
        long currentDispatchTableOffset = coreDispatchTableOffset;

        // 按照CoreHMTable元素在CoreHMTableList下的顺序依次生成
        for (Map.Entry<String, CoreHMTable> entry : coreHMTables.entrySet()) {
            CoreHMTable coreHMTable = entry.getValue();

            // 计算分派表起始地址
            long dispatchTableStartAddr = cpmStartAddress + currentDispatchTableOffset;

            // 计算分派表条目数（使用与CoreDispatchTableConfigGenerator64small一致的逻辑）
            int dispatchTableEntryCount = 0;

            if (coreHMTable.getErrorActions() != null) {
                // 获取该 CoreHMTable 对应的所有 StateIdentifier
                List<Integer> stateIdentifiers = resolveStateIdentifiers(coreHMTable, module.getfHealthMonitor());

                // 为每个 ErrorAction 计算生成的结构体数量
                for (CoreHMTable.ErrorAction errorAction : coreHMTable.getErrorActions()) {
                    dispatchTableEntryCount += stateIdentifiers.size();
                }
            }

            // 生成该表的二进制数据
            byte[] tableData = generateBinary(coreHMTable, fieldMap, dispatchTableStartAddr, dispatchTableEntryCount);
            baos.write(tableData);

            // 更新下一个表的偏移地址，计算当前CoreHMTable对应的分派表大小
            int currentTableDispatchSize = CoreDispatchTableConfigGenerator64small.calculateSizeForSingleTable(
                    coreHMTable, module.getfHealthMonitor());
            currentDispatchTableOffset += currentTableDispatchSize;
        }

        return baos.toByteArray();
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算核心健康监控表配置的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getCoreHMTables() == null ||
                module.getfHealthMonitor().getCoreHMTables().isEmpty()) {
            return 0;
        }

        Map<String, CoreHMTable> coreHMTables = module.getfHealthMonitor().getCoreHMTables();

        // 使用8字节对齐的单个表配置大小
        int singleTableSize = calculateSingleTableSize();

        return coreHMTables.size() * singleTableSize;
    }

    /**
     * calculateSingleTableSize
     *
     * @brief
     * 功能: 计算单个核心健康监控表配置结构的大小 (32位系统)。
     *
     * @return 单个表配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleTableSize() {
        // 配置类型(4) + 配置数据大小(4) + 表名(32) + 分派表起始地址(4，32位) +
        // 分派表条目数(4) + 核心ID数组(4*255) + 分区数目(4) = 1068字节
        int baseSize = 4 + 4 + 32 + 8 + 4 + (4 * CORE_ID_ARRAY_SIZE) + 4;

        // 计算8字节对齐后的大小
        return calculateAlignedSize(baseSize, 8);
    }

    /**
     * calculateAlignmentPadding
     *
     * @brief
     * 功能: 计算8字节对齐所需的填充字节数。
     *
     * @param[in]  currentSize: 当前大小
     * @return 需要填充的字节数
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateAlignmentPadding(int currentSize) {
        int remainder = currentSize % 8;
        return remainder == 0 ? 0 : (8 - remainder);
    }

    /**
     * calculateAlignedSize
     *
     * @brief
     * 功能: 计算8字节对齐后的大小。
     *
     * @param[in]  baseSize:  基础大小
     * @param[in]  alignment: 对齐字节数
     * @return 对齐后的大小
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateAlignedSize(int baseSize, int alignment) {
        int remainder = baseSize % alignment;
        return remainder == 0 ? baseSize : baseSize + (alignment - remainder);
    }

    /**
     * resolveStateIdentifiers
     *
     * @brief
     * 功能: 先计算所有 CoreHmState 的 StateIdentifier 位或值。如果位或结果覆盖 CoreHMTable 的 StateIdentifier，则将 CoreHMTable 的值按位从高到低拆分；否则返回 CoreHMTable 的原始 StateIdentifier。。
     *
     * @param[in]  coreHMTable:   CoreHMTable 对象
     * @param[in]  healthMonitor: HealthMonitor 对象，用于获取 CoreHmState 列表
     * @return StateIdentifier 列表
     * @scope internal
     * @errno:
     * N/A
     */
    private static List<Integer> resolveStateIdentifiers(CoreHMTable coreHMTable,
            com.nwpu.verify.entity.HealthMonitor healthMonitor) {
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
