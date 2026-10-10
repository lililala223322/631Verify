/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: HealthMonitorConfigGenerator32small.java
 * 
 * 描述    : 生成HealthMonitorConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-30  开发人员  按表7扩展健康监控配置五组地址数组容量。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.PartitionHMTable;
import com.nwpu.verify.entity.CoreHMTask;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 健康监控配置数据存储结构 (32位大端)。
 */
public class HealthMonitorConfigGenerator32small {

    private static final int CORE_HM_TABLE_ARRAY_SIZE = 160;
    private static final int MULTI_PARTITION_HM_TABLE_ARRAY_SIZE = 255;
    private static final int PARTITION_HM_TABLE_ARRAY_SIZE = 255;
    private static final int CORE_HM_TASK_ARRAY_SIZE = 16;
    private static final int PARTITION_HM_TASK_ARRAY_SIZE = 255;

    /**
     * generate
     *
     * @brief
     * 功能: 生成健康监控配置存储结构的二进制数据。
     *
     * @param[in]  module: 模块对象，包含健康监控配置信息
     * @param[in]  fieldMap: 字段映射表，用于记录生成的二进制字段
     * @param[in]  cpmStartAddress: CPM配置信息可加载文件起始地址
     * @param[in]  coreHealthMonitorTableConfigOffset: 核心健康监控表配置在文件中的偏移
     * @param[in]  multiPartitionHealthMonitorTableConfigOffset: 多分区健康监控表配置在文件中的偏移
     * @param[in]  partitionHealthMonitorTableConfigOffset: 分区健康监控表配置在文件中的偏移
     * @param[in]  healthMonitorConfigOffset: 核心健康监控任务配置在文件中的偏移
     * @param[in]  partitionHealthMonitorConfigOffset: 分区健康监控任务配置在文件中的偏移
     * @return 健康监控配置存储结构的二进制数据
     * @throws IOException 写入异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap, 
            long cpmStartAddress, long coreHealthMonitorTableConfigOffset, 
            long multiPartitionHealthMonitorTableConfigOffset, long partitionHealthMonitorTableConfigOffset,
            long healthMonitorConfigOffset, long partitionHealthMonitorConfigOffset) throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // 1. 生成核心健康监控表数组和计数 (32字节数组 + 4字节count)
        generateCoreHMTableSection(module, fieldMap, baos, cpmStartAddress, coreHealthMonitorTableConfigOffset);
        // 注意：根据数据词典表7，各section之间紧密排列，不需要内部padding

        // 2. 生成多分区健康监控表数组和计数 (32字节数组 + 4字节count)
        generateMultiPartitionHMTableSection(module, fieldMap, baos, cpmStartAddress, multiPartitionHealthMonitorTableConfigOffset);

        // 3. 生成分区健康监控表数组和计数 (32字节数组 + 4字节count)
        generatePartitionHMTableSection(module, fieldMap, baos, cpmStartAddress, partitionHealthMonitorTableConfigOffset);

        // 4. 生成核心健康监控任务数组和计数 (32字节数组 + 4字节count)
        generateCoreHMTaskSection(module, fieldMap, baos, cpmStartAddress, healthMonitorConfigOffset);
        
        // 5. 生成分区健康监控任务数组和计数 (32字节数组 + 4字节count)
        generatePartitionHMTaskSection(module, fieldMap, baos, cpmStartAddress, partitionHealthMonitorConfigOffset);
        
        // 整个结构末尾进行8字节对齐（32位：5*(32+4)=180字节，需要4字节padding）
        add8ByteAlignmentIfNeeded(baos, fieldMap, "HealthMonitorConfig_Padding");
        
        return baos.toByteArray();
    }

    /**
     * generateCoreHMTableSection
     *
     * @brief
     * 功能: 生成核心健康监控表数组和配置个数。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateCoreHMTableSection(Module module, Map<String, byte[]> fieldMap, 
            ByteArrayOutputStream baos, long cpmStartAddress, long tableConfigOffset) throws IOException {
        
        // 获取核心健康监控表的数量
        int coreHMTableCount = 0;
        if (module.getfHealthMonitor() != null && module.getfHealthMonitor().getCoreHMTables() != null) {
            coreHMTableCount = module.getfHealthMonitor().getCoreHMTables().size();
        }
        
        // 核心健康监控表数组
        for (int i = 0; i < CORE_HM_TABLE_ARRAY_SIZE; i++) {
            long tableAddress;
            if (i < coreHMTableCount) {
                // 计算核心健康监控表在文件中的地址
                tableAddress = cpmStartAddress + tableConfigOffset + (i * calculateSingleCoreHMTableSize());
            } else {
                // 未配置的表设置为0
                tableAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.intToBytesLittleEndian((int) tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_CoreHMTable_" + i, addressBytes);
        }
        
        // 核心健康监控表配置个数 (4字节)
        byte[] coreHMTableCountBytes = BinaryUtils.longTo4BytesLittleEndian(coreHMTableCount);
        baos.write(coreHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_CoreHMTableCount", coreHMTableCountBytes);
    }

    /**
     * generateMultiPartitionHMTableSection
     *
     * @brief
     * 功能: 生成多分区健康监控表数组和配置个数。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateMultiPartitionHMTableSection(Module module, Map<String, byte[]> fieldMap, 
            ByteArrayOutputStream baos, long cpmStartAddress, long tableConfigOffset) throws IOException {
        
        // 获取多分区健康监控表的数量
        int multiPartitionHMTableCount = 0;
        if (module.getfHealthMonitor() != null && module.getfHealthMonitor().getMultiPartitionHMTables() != null) {
            multiPartitionHMTableCount = module.getfHealthMonitor().getMultiPartitionHMTables().size();
        }
        
        // 多分区健康监控表数组
        for (int i = 0; i < MULTI_PARTITION_HM_TABLE_ARRAY_SIZE; i++) {
            long tableAddress;
            if (i < multiPartitionHMTableCount) {
                // 计算多分区健康监控表在文件中的地址
                tableAddress = cpmStartAddress + tableConfigOffset + (i * calculateSingleMultiPartitionHMTableSize());
            } else {
                // 未配置的表设置为0
                tableAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.intToBytesLittleEndian((int) tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_MultiPartitionHMTable_" + i, addressBytes);
        }
        
        // 多分区健康监控表配置个数 (4字节)
        byte[] multiPartitionHMTableCountBytes = BinaryUtils.longTo4BytesLittleEndian(multiPartitionHMTableCount);
        baos.write(multiPartitionHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_MultiPartitionHMTableCount", multiPartitionHMTableCountBytes);
    }

    /**
     * generatePartitionHMTableSection
     *
     * @brief
     * 功能: 生成分区健康监控表数组和配置个数。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generatePartitionHMTableSection(Module module, Map<String, byte[]> fieldMap, 
            ByteArrayOutputStream baos, long cpmStartAddress, long tableConfigOffset) throws IOException {
        
        // 获取分区健康监控表的数量和实际表数据
        int partitionHMTableCount = 0;
        Map<String, PartitionHMTable> partitionHMTables = null;
        if (module.getfHealthMonitor() != null && module.getfHealthMonitor().getPartitionHMTables() != null) {
            partitionHMTables = module.getfHealthMonitor().getPartitionHMTables();
            partitionHMTableCount = partitionHMTables.size();
        }
        
        // 分区健康监控表数组
        int tableIndex = 0;
        long currentOffset = tableConfigOffset;
        
        for (int i = 0; i < PARTITION_HM_TABLE_ARRAY_SIZE; i++) {
            long tableAddress;
            if (i < partitionHMTableCount && partitionHMTables != null) {
                // 计算分区健康监控表在文件中的地址
                // 每个表的偏移是累积计算的，因为不同表的大小可能不同
                tableAddress = cpmStartAddress + currentOffset;
                
                // 更新偏移量，为下一个表做准备
                if (tableIndex < partitionHMTableCount - 1) {
                    currentOffset += calculateSinglePartitionHMTableSize();
                }
                tableIndex++;
            } else {
                // 未配置的表设置为0
                tableAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.intToBytesLittleEndian((int) tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_PartitionHMTable_" + i, addressBytes);
        }
        
        // 分区健康监控表配置个数 (4字节)
        byte[] partitionHMTableCountBytes = BinaryUtils.longTo4BytesLittleEndian(partitionHMTableCount);
        baos.write(partitionHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_PartitionHMTableCount", partitionHMTableCountBytes);
    }

    /**
     * generateCoreHMTaskSection
     *
     * @brief
     * 功能: 生成核心健康监控任务数组和配置个数。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateCoreHMTaskSection(Module module, Map<String, byte[]> fieldMap, 
            ByteArrayOutputStream baos, long cpmStartAddress, long taskConfigOffset) throws IOException {
        
        // 获取核心健康监控任务的数量和实际任务数据
        int coreHMTaskCount = 0;
        Map<String, CoreHMTask> coreHMTasks = null;
        if (module.getfHealthMonitor() != null && module.getfHealthMonitor().getfCoreHMTasks() != null) {
            coreHMTasks = module.getfHealthMonitor().getfCoreHMTasks();
            coreHMTaskCount = coreHMTasks.size();
        }
        
        // 核心健康监控任务数组
        int taskIndex = 0;
        long currentOffset = taskConfigOffset;
        
        for (int i = 0; i < CORE_HM_TASK_ARRAY_SIZE; i++) {
            long taskAddress;
            if (i < coreHMTaskCount && coreHMTasks != null) {
                // 计算核心健康监控任务在文件中的地址
                taskAddress = cpmStartAddress + currentOffset;
                
                // 更新偏移量，为下一个任务做准备
                if (taskIndex < coreHMTaskCount - 1) {
                    currentOffset += calculateSingleCoreHMTaskSize();
                }
                taskIndex++;
            } else {
                // 未配置的任务设置为0
                taskAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.intToBytesLittleEndian((int) taskAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_CoreHMTask_" + i, addressBytes);
        }
        
        // 核心健康监控任务配置个数 (4字节)
        byte[] coreHMTaskCountBytes = BinaryUtils.longTo4BytesLittleEndian(coreHMTaskCount);
        baos.write(coreHMTaskCountBytes);
        fieldMap.put("HealthMonitorConfig_CoreHMTaskCount", coreHMTaskCountBytes);
    }

    /**
     * generatePartitionHMTaskSection
     *
     * @brief
     * 功能: 生成分区健康监控任务数组和配置个数。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generatePartitionHMTaskSection(Module module, Map<String, byte[]> fieldMap, 
            ByteArrayOutputStream baos, long cpmStartAddress, long taskConfigOffset) throws IOException {
        
        // 获取分区健康监控任务的数量
        int partitionHMTaskCount = 0;
        if (module.getfHealthMonitor() != null && module.getfHealthMonitor().getfPartitionHMTasks() != null) {
            partitionHMTaskCount = module.getfHealthMonitor().getfPartitionHMTasks().size();
        }
        
        // 分区健康监控任务数组
        for (int i = 0; i < PARTITION_HM_TASK_ARRAY_SIZE; i++) {
            long taskAddress;
            if (i < partitionHMTaskCount) {
                // 计算分区健康监控任务在文件中的地址
                // 每个任务后有8字节对齐填充（1132→1136），用对齐后的大小计算偏移
                int singleTaskSize = calculateSinglePartitionHMTaskSize();
                int alignedTaskSize = (singleTaskSize + 7) & ~7; // 1136
                taskAddress = cpmStartAddress + taskConfigOffset + ((long) i * alignedTaskSize);
            } else {
                // 未配置的任务设置为0
                taskAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.intToBytesLittleEndian((int) taskAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_PartitionHMTask_" + i, addressBytes);
        }
        
        // 分区健康监控任务配置个数 (4字节)
        byte[] partitionHMTaskCountBytes = BinaryUtils.longTo4BytesLittleEndian(partitionHMTaskCount);
        baos.write(partitionHMTaskCountBytes);
        fieldMap.put("HealthMonitorConfig_PartitionHMTaskCount", partitionHMTaskCountBytes);
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算健康监控配置存储结构的总大小根据数据词典表7，字段紧密排列，无内部padding。
     *
     * @param[in]  module: 模块对象
     * @return 健康监控配置存储结构的大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        int totalSize = (CORE_HM_TABLE_ARRAY_SIZE
                + MULTI_PARTITION_HM_TABLE_ARRAY_SIZE
                + PARTITION_HM_TABLE_ARRAY_SIZE
                + CORE_HM_TASK_ARRAY_SIZE
                + PARTITION_HM_TASK_ARRAY_SIZE) * 4;
        totalSize += 5 * 4; // 五个count字段
        int padding = (8 - (totalSize % 8)) % 8;
        totalSize += padding;

        return totalSize;
    }

    /**
     * add8ByteAlignmentIfNeeded
     *
     * @brief
     * 功能: 在8字节字段前检查并添加对齐如果当前位置不是8字节对齐，就添加4字节padding。
     *
     * @param[in]  baos: 字节输出流
     * @param[in]  fieldMap: 字段映射表
     * @param[in]  paddingName: padding字段的名称
     * @throws IOException 写入异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static void add8ByteAlignmentIfNeeded(ByteArrayOutputStream baos, 
            Map<String, byte[]> fieldMap, String paddingName) throws IOException {
        int currentSize = baos.size();
        
        // 检查是否8字节对齐
        if (currentSize % 8 != 0) {
            // 未对齐，需要添加padding
            int paddingSize = 8 - (currentSize % 8);
            byte[] paddingBytes = new byte[paddingSize];
            baos.write(paddingBytes);
            fieldMap.put(paddingName, paddingBytes);
        }
    }

    // ===== 私有辅助方法：计算各种表/任务的单个结构大小 =====

    /**
     * calculateSingleCoreHMTableSize
     *
     * @brief
     * 功能: 计算单个核心健康监控表结构的大小使用 CoreHMTableGenerator32small 计算实际的表结构大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSingleCoreHMTableSize() {
        return CoreHMTableGenerator32small.calculateSingleTableSize();
    }

    /**
     * calculateSingleMultiPartitionHMTableSize
     *
     * @brief
     * 功能: 计算单个多分区健康监控表结构的大小使用 MultiPartitionHMTableGenerator32small 计算实际的表结构大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSingleMultiPartitionHMTableSize() {
        return MultiPartitionHMTableGenerator32small.calculateSingleTableSize();
    }

    /**
     * calculateSinglePartitionHMTableSize
     *
     * @brief
     * 功能: 计算单个分区健康监控表结构的大小使用 PartitionHealthMonitorTableConfigGenerator32small 计算实际的表结构大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSinglePartitionHMTableSize() {
        return PartitionHealthMonitorTableConfigGenerator32small.calculateSingleTableConfigSize();
    }

    /**
     * calculateSingleCoreHMTaskSize
     *
     * @brief
     * 功能: 计算单个核心健康监控任务结构的大小使用 CoreHMTaskGenerator32small 计算实际的任务结构大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSingleCoreHMTaskSize() {
        return CoreHMTaskGenerator32small.calculateSingleTaskSize();
    }

    /**
     * calculateSinglePartitionHMTaskSize
     *
     * @brief
     * 功能: 计算单个分区健康监控任务结构的大小使用 PartitionHMTaskGenerator32small 计算实际的任务结构大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSinglePartitionHMTaskSize() {
        return PartitionHMTaskGenerator32small.calculateSinglePartitionHMTaskSize();
    }
}

