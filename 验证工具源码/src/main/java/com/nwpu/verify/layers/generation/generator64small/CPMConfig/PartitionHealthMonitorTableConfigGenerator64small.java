/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionHealthMonitorTableConfigGenerator64small.java
 * 
 * 描述    : 生成PartitionHealthMonitorTableConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.HealthMonitor;
import com.nwpu.verify.entity.PartitionHMTable;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 分区健康监控表配置存储结构生成器 (32位大端)根据 PartitionHMTable 生成分区健康监控表配置存储。
 */
public class PartitionHealthMonitorTableConfigGenerator64small {

    // 配置类型固定值
    private static final int CONFIG_TYPE = 1129468933;

    // 分区ID数组最大长度
    private static final int MAX_PARTITION_IDS = 255;

    /**
     * calculatePaddingFor8ByteAlignment
     *
     * @brief
     * 功能: 计算8字节对齐所需的填充字节数。
     *
     * @param[in]  currentSize: 当前大小
     * @return 需要的填充字节数
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculatePaddingFor8ByteAlignment(int currentSize) {
        int remainder = currentSize % 8;
        return remainder == 0 ? 0 : (8 - remainder);
    }

    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 将大小调整为8字节对齐。
     *
     * @param[in]  size: 原始大小
     * @return 8字节对齐后的大小
     * @scope internal
     * @errno:
     * N/A
     */
    private static int alignTo8Bytes(int size) {
        return size + calculatePaddingFor8ByteAlignment(size);
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成分区健康监控表配置存储结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 从XML解析层传入的顶层Module对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射
     * @param[in]  cpmStartAddress: CPM配置信息可加载文件起始地址
     * @param[in]  partitionDispatchTableOffset: 分区分派表在文件中的偏移量
     * @return 生成的分区健康监控表配置存储结构二进制字节数。。
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress, long partitionDispatchTableOffset) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            HealthMonitor healthMonitor = module.getfHealthMonitor();
            if (healthMonitor == null) {
                return baos.toByteArray(); // 如果没有健康监控配置，返回空数组
            }

            Map<String, PartitionHMTable> partitionHMTables = healthMonitor.getPartitionHMTables();
            if (partitionHMTables == null || partitionHMTables.isEmpty()) {
                return baos.toByteArray(); // 如果没有分区健康监控表，返回空数组
            }

            int tableIndex = 0;
            long currentDispatchTableOffset = partitionDispatchTableOffset;

            // 遍历所有 PartitionHMTable
            for (PartitionHMTable partitionHMTable : partitionHMTables.values()) {
                // 计算当前表的配置数据大小
                int configDataSize = calculateSingleTableConfigSize();

                // 字段1: 配置类型 (4字节)
                // 固定值正整数"1129468933"
                byte[] configTypeBytes = BinaryUtils.intToBytesLittleEndian(CONFIG_TYPE);
                baos.write(configTypeBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].ConfigType", configTypeBytes);

                // 字段2: 配置数据大小 (4字节)
                // 分区健康监控配置数据结构占用空间的大小
                byte[] configSizeBytes = BinaryUtils.intToBytesLittleEndian(configDataSize);
                baos.write(configSizeBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].ConfigDataSize", configSizeBytes);

                // 字段3: 表名 (32字节字符数组)
                // TableName 的属性值，取值长度不。2时以字符"\0"填充直至长度达到32
                String tableName = partitionHMTable.getTableNameAsString();
                byte[] tableNameBytes = BinaryUtils.fixedLengthAscii(tableName, 32, '\0');
                baos.write(tableNameBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].TableName", tableNameBytes);

                // 字段4: 分派表起始地址 (4字节，32位系统)
                // CPM配置信息可加载文件起始地址+本分区健康监控表配置结构体的第一个分区分派表在CPM配置信息可加载文件中的偏移
                long dispatchTableStartAddress = cpmStartAddress + currentDispatchTableOffset;
                byte[] dispatchTableStartBytes = BinaryUtils.longTo8BytesLittleEndian(dispatchTableStartAddress);
                baos.write(dispatchTableStartBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].DispatchTableStartAddress",
                        dispatchTableStartBytes);

                // 字段5: 分派表条数目 (4字节)
                // 分区分派表个。- 根据当前表的ErrorAction数量计算
                int dispatchTableCount = partitionHMTable.getErrorActions() != null
                        ? partitionHMTable.getErrorActions().size()
                        : 0;
                byte[] dispatchTableCountBytes = BinaryUtils.intToBytesLittleEndian(dispatchTableCount);
                baos.write(dispatchTableCountBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].DispatchTableCount",
                        dispatchTableCountBytes);

                // 字段6: 使用该健康监控表的分区ID (4*255字节数组)
                // 数组个数为255，赋值为PartitionRef元素的Id属性值
                byte[] partitionIdsBytes = generatePartitionIdsArray(partitionHMTable, tableIndex, fieldMap);
                baos.write(partitionIdsBytes);

                // 字段7: 使用该健康监控表的分区数目(4字节)
                // PartitionHMTable元素下子元素PartitionRef的个数
                int partitionCount = partitionHMTable.getPartitionRefs() != null
                        ? partitionHMTable.getPartitionRefs().size()
                        : 0;
                byte[] partitionCountBytes = BinaryUtils.intToBytesLittleEndian(partitionCount);
                baos.write(partitionCountBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].PartitionCount", partitionCountBytes);

                // 字段8: 动态填充字节，用于8字节对齐
                int baseSize = 4 + 4 + 32 + 8 + 4 + (4 * MAX_PARTITION_IDS) + 4; // 基础结构大小 (分派表起始地址32位是4字节)
                int paddingSize = calculatePaddingFor8ByteAlignment(baseSize);
                if (paddingSize > 0) {
                    byte[] padding = new byte[paddingSize];
                    baos.write(padding);
                    fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].Padding", padding);
                }

                // 更新偏移量（为下一个表准备）
                // 假设每个分区分派表条目占用固定字节数，这里需要根据实际的分区分派表结构调整
                currentDispatchTableOffset += dispatchTableCount * getPartitionDispatchTableEntrySize();

                tableIndex++;
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
     * 功能: 计算分区健康监控表配置存储结构所需的总字节数。
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

        Map<String, PartitionHMTable> partitionHMTables = healthMonitor.getPartitionHMTables();
        if (partitionHMTables == null || partitionHMTables.isEmpty()) {
            return 0;
        }

        // 每个分区健康监控表配置存储结构占用（8字节对齐）：
        // 配置类型(4) + 配置数据大小(4) + 表名(32) + 分派表起始地址(8) + 
        // 分派表条数目(4) + 分区ID数组(4*255) + 分区数目(4) + 动态填充字节
        int singleTableSize = calculateSingleTableConfigSize();
        return partitionHMTables.size() * singleTableSize;
    }

    /**
     * calculateSingleTableConfigSize
     *
     * @brief
     * 功能: 计算单个分区健康监控表配置结构的大小（8字节对齐，32位系统）。
     *
     * @return 单个表配置结构的字节数
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleTableConfigSize() {
        // 配置类型(4) + 配置数据大小(4) + 表名(32) + 分派表起始地址(4，32位) + 
        // 分派表条数目(4) + 分区ID数组(4*255) + 分区数目(4)
        int baseSize = 4 + 4 + 32 + 8 + 4 + (4 * MAX_PARTITION_IDS) + 4;

        // 返回8字节对齐后的大小
        return alignTo8Bytes(baseSize);
    }

    /**
     * generatePartitionIdsArray
     *
     * @brief
     * 功能: 生成分区ID数组 (4*255字节)。
     *
     * @param[in]  partitionHMTable: 分区健康监控表
     * @param[in]  tableIndex: 表索引
     * @param[in]  fieldMap: 字段映射
     * @return 分区ID数组字节数据
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generatePartitionIdsArray(PartitionHMTable partitionHMTable,
            int tableIndex, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 获取PartitionRef列表
            int partitionRefCount = 0;
            if (partitionHMTable.getPartitionRefs() != null) {
                partitionRefCount = partitionHMTable.getPartitionRefs().size();
            }

            // 填充实际的分区ID
            for (int i = 0; i < MAX_PARTITION_IDS; i++) {
                int partitionId = 0; // 默认值

                if (i < partitionRefCount) {
                    // 使用实际的分区ID
                    partitionId = partitionHMTable.getPartitionRefs().get(i).getId();
                }
                // 否则保持默认值0

                byte[] idBytes = BinaryUtils.intToBytesLittleEndian(partitionId);
                baos.write(idBytes);
                fieldMap.put("PartitionHealthMonitorTable[" + tableIndex + "].PartitionIds[" + i + "]", idBytes);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[4 * MAX_PARTITION_IDS];
        }
    }

    /**
     * getPartitionDispatchTableEntrySize
     *
     * @brief
     * 功能: 获取单个分区分派表条目的大小根据PartitionDispatchTableGenerator64small的实现：系统状态(4) + 分派级别(4) + 故障代码(4) + 处理动作名称(64) = 76字节。
     *
     * @return 分区分派表条目大小
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getPartitionDispatchTableEntrySize() {
        return 76; // 系统状态(4) + 分派级别(4) + 故障代码(4) + 处理动作名称(64) = 76字节
    }
}
