/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MultiPartitionHMTableGenerator64small.java
 * 
 * 描述    : 生成MultiPartitionHMTable相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.HealthMonitor;
import com.nwpu.verify.entity.MultiPartitionHMTable;
import com.nwpu.verify.entity.PartitionHMTable;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @brief
 * 功能: 4.1.3.3 多分区健康监控表配置存储结构 生成器 (32位大端)。
 */
public class MultiPartitionHMTableGenerator64small {

    /**
     * intToBytesLittleEndian
     *
     * @brief
     * 功能: 执行 intToBytesLittleEndian 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] intToBytesLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

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
     * stringTo32BytesPadded
     *
     * @brief
     * 功能: 执行 stringTo32BytesPadded 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] stringTo32BytesPadded(String s) {
        byte[] bytes = new byte[32];
        if (s != null) {
            byte[] sBytes = s.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(sBytes, 0, bytes, 0, Math.min(sBytes.length, 32));
        }
        return bytes;
    }

    /**
     * generateMultiPartitionHMTables
     *
     * @brief
     * 功能: 生成所有“多分区健康监控表”的二进制内容。。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象。。
     * @param[in]  fieldMap: 用于记录字段映射。。
     * @param[in]  cpmStartAddress: CPM配置信息可加载文件的起始地址。。
     * @param[in]  multiPartitionDispatchTableOffset: 第一个分派表在文件中的偏移量。。
     * @return 所有多分区健康监控表配置的二进制字节数组。。
     * @throws IOException 写入流时可能发生异常。。
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateMultiPartitionHMTables(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress, long multiPartitionDispatchTableOffset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HealthMonitor healthMonitor = module.getfHealthMonitor();
        if (healthMonitor == null || healthMonitor.getMultiPartitionHMTables() == null
                || healthMonitor.getMultiPartitionHMTables().isEmpty()) {
            return baos.toByteArray();
        }

        long currentDispatchOffset = multiPartitionDispatchTableOffset;

        for (MultiPartitionHMTable table : healthMonitor.getMultiPartitionHMTables().values()) {
            String baseFieldName = "MultiPartitionHMTable_" + new String(table.getName()) + "_";

            // 1. 配置类型 (fConfigType) - 4字节, 固定。1129468933
            final int CONFIG_TYPE = 1129468933;
            baos.write(intToBytesLittleEndian(CONFIG_TYPE));
            fieldMap.put(baseFieldName + "ConfigType", intToBytesLittleEndian(CONFIG_TYPE));

            // 2. 配置数据大小 (fConfigDataSize) - 4字节
            // ConfigDataSize设置为总结构大小，包含配置类型和配置数据大小字段
            int totalStructSize = calculateSingleTableSize(); // 整个结构的总大小，包含8字节对齐
            baos.write(intToBytesLittleEndian(totalStructSize));
            fieldMap.put(baseFieldName + "ConfigDataSize", intToBytesLittleEndian(totalStructSize));

            // 3. 表名 (fTableName) - 32字节
            baos.write(stringTo32BytesPadded(new String(table.getName())));
            fieldMap.put(baseFieldName + "TableName", stringTo32BytesPadded(new String(table.getName())));

            // 4. 分派表起始地址 (fDispatchTableStartAddress) - 4字节 (32位系统)
            long dispatchTableAddress = cpmStartAddress + currentDispatchOffset;
            baos.write(BinaryUtils.longTo8BytesLittleEndian(dispatchTableAddress));
            fieldMap.put(baseFieldName + "DispatchTableStartAddress",
                    BinaryUtils.longTo8BytesLittleEndian(dispatchTableAddress));

            // 5. 分派表条数目 (fDispatchTableEntryCount) - 4字节
            int dispatchTableCount = table.getErrorActions().size();
            baos.write(intToBytesLittleEndian(dispatchTableCount));
            fieldMap.put(baseFieldName + "DispatchTableEntryCount", intToBytesLittleEndian(dispatchTableCount));

            // 6. 使用该健康监控表的分区ID (fPartitionIds) - 4 * 255 字节
            // 使用LinkedHashSet去重，同时保持插入顺序
            Set<Long> partitionIdSet = new LinkedHashSet<>();

            // 查找所有MultiPartitionHMTableNameRef属性值为本多分区健康监控表名的PartitionHMTable
            if (healthMonitor.getPartitionHMTables() != null) {
                String currentTableName = new String(table.getName());
                for (PartitionHMTable partitionHMTable : healthMonitor.getPartitionHMTables().values()) {
                    // 检查MultiPartitionHMTableNameRef是否匹配当前多分区健康监控表。
                    if (currentTableName.equals(new String(partitionHMTable.getMultiPartitionHMTableNameRef()))) {
                        // 收集该PartitionHMTable引用的所有PartitionRef元素的Id属性。
                        if (partitionHMTable.getPartitionRefs() != null) {
                            for (PartitionHMTable.PartitionRef partitionRef : partitionHMTable.getPartitionRefs()) {
                                // LinkedHashSet自动去重
                                partitionIdSet.add((long) partitionRef.getId());
                            }
                        }
                    }
                }
            }

            // 转换为List以便按索引访问
            List<Long> partitionIds = new ArrayList<>(partitionIdSet);

            // 生成4*255字节的分区ID数组
            byte[] partitionIdBytes = new byte[4 * 255];
            ByteBuffer idBuffer = ByteBuffer.wrap(partitionIdBytes).order(ByteOrder.LITTLE_ENDIAN);

            // 填充分区ID数组：数组个数为255，当PartitionRef元素个数少于255时，剩余数组成员赋值固定值正整数"0"
            for (int i = 0; i < 255; i++) {
                long id = (i < partitionIds.size()) ? partitionIds.get(i) : 0L;
                idBuffer.putInt((int) id);
            }
            baos.write(partitionIdBytes);
            fieldMap.put(baseFieldName + "PartitionIds", partitionIdBytes);

            // 7. 使用该健康监控表的分区数。(fPartitionCount) - 4字节
            baos.write(intToBytesLittleEndian(partitionIds.size()));
            fieldMap.put(baseFieldName + "PartitionCount", intToBytesLittleEndian(partitionIds.size()));

            // 8. 动态填充字节，用于8字节对齐
            // 填充大小应该与配置数据大小的计算保持一致
            int actualDataSize = 32 + 8 + 4 + (4 * 255) + 4; // 实际数据大小（表名到分区数目，分派表起始地址32位是4字节）
            int paddingSize = calculatePaddingFor8ByteAlignment(actualDataSize);
            if (paddingSize > 0) {
                byte[] padding = new byte[paddingSize];
                baos.write(padding);
                fieldMap.put(baseFieldName + "Padding", padding);
            }

            // 更新下一个多分区健康监控表的分派表偏移量
            // 每个多分区分派表条目大小。6字节（系统状。 + 分派级别4 + 故障代码4 + 处理动作名称64。
            currentDispatchOffset += dispatchTableCount * 76;
        }

        return baos.toByteArray();
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算多分区健康监控表配置的总大。。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 模块对象
     * @return 配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getMultiPartitionHMTables() == null ||
                module.getfHealthMonitor().getMultiPartitionHMTables().isEmpty()) {
            return 0;
        }

        Map<String, MultiPartitionHMTable> multiPartitionHMTables = module.getfHealthMonitor()
                .getMultiPartitionHMTables();

        // 每个多分区健康监控表配置结构的大小（动态8字节对齐）：
        // 配置类型(4) + 配置数据大小(4) + 表名(32) + 分派表起始地址(8) + 
        // 分派表条目数(4) + 分区ID数组(4*255) + 分区数目(4) + 动态填充字节
        int singleTableSize = calculateSingleTableSize();

        return multiPartitionHMTables.size() * singleTableSize;
    }

    /**
     * calculateSingleTableSize
     *
     * @brief
     * 功能: 计算单个多分区健康监控表配置结构的大小 (32位系统)。
     *
     * @return 单个表配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleTableSize() {
        // 根据规格说明的完整结构大小 (32位系统)：
        // 配置类型(4) + 配置数据大小(4) + 数据内容 + 动态填充
        int headerSize = 4 + 4; // 配置类型 + 配置数据大小
        int dataSize = 32 + 8 + 4 + (4 * 255) + 4; // 表名 + 分派表起始地址(4字节，32位) + 分派表条目数 + 分区ID数组 + 分区数目

        // 计算整个结构的8字节对齐大小
        int totalSize = headerSize + dataSize;
        return alignTo8Bytes(totalSize);
    }
}
