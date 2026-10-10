/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: HealthMonitorConfigGenerator64big.java
 * 
 * 描述    : 生成Health Monitor相关64位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-30  开发人员  按表7扩展健康监控配置五组地址数组容量。
 */
package com.nwpu.verify.layers.generation.generator64big.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.PartitionHMTable;
import com.nwpu.verify.entity.CoreHMTask;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * 健康监控配置数据存储结构 (64位大端)
 *
 * 五组地址数组容量依次为160、255、255、16、255，
 * 每组数组后跟4字节配置个数并按8字节对齐。
 */
public class HealthMonitorConfigGenerator64big {

    private static final int CORE_HM_TABLE_ARRAY_SIZE = 160;
    private static final int MULTI_PARTITION_HM_TABLE_ARRAY_SIZE = 255;
    private static final int PARTITION_HM_TABLE_ARRAY_SIZE = 255;
    private static final int CORE_HM_TASK_ARRAY_SIZE = 16;
    private static final int PARTITION_HM_TASK_ARRAY_SIZE = 255;

    /**
     * 生成健康监控配置存储结构的二进制数据
     * 
     * @param module 模块对象，包含健康监控配置信息
     * @param fieldMap 字段映射表，用于记录生成的二进制字段
     * @param cpmStartAddress CPM配置信息可加载文件起始地址
     * @param coreHealthMonitorTableConfigOffset 核心健康监控表配置在文件中的偏移
     * @param multiPartitionHealthMonitorTableConfigOffset 多分区健康监控表配置在文件中的偏移
     * @param partitionHealthMonitorTableConfigOffset 分区健康监控表配置在文件中的偏移
     * @param healthMonitorConfigOffset 核心健康监控任务配置在文件中的偏移
     * @param partitionHealthMonitorConfigOffset 分区健康监控任务配置在文件中的偏移
     * @return 健康监控配置存储结构的二进制数据
     * @throws IOException 写入异常
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap, 
            long cpmStartAddress, long coreHealthMonitorTableConfigOffset, 
            long multiPartitionHealthMonitorTableConfigOffset, long partitionHealthMonitorTableConfigOffset,
            long healthMonitorConfigOffset, long partitionHealthMonitorConfigOffset) throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // 1. 生成核心健康监控表数组和计数 (64字节数组 + 4字节count)
        generateCoreHMTableSection(module, fieldMap, baos, cpmStartAddress, coreHealthMonitorTableConfigOffset);
        // 在下一个8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreHMTable_Padding");

        // 2. 生成多分区健康监控表数组和计数 (64字节数组 + 4字节count)
        generateMultiPartitionHMTableSection(module, fieldMap, baos, cpmStartAddress, multiPartitionHealthMonitorTableConfigOffset);
        // 在下一个8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "MultiPartitionHMTable_Padding");

        // 3. 生成分区健康监控表数组和计数 (64字节数组 + 4字节count)
        generatePartitionHMTableSection(module, fieldMap, baos, cpmStartAddress, partitionHealthMonitorTableConfigOffset);
        // 在下一个8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "PartitionHMTable_Padding");

        // 4. 生成核心健康监控任务数组和计数 (64字节数组 + 4字节count)
        generateCoreHMTaskSection(module, fieldMap, baos, cpmStartAddress, healthMonitorConfigOffset);
        // 在下一个8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreHMTask_Padding");
        
        // 5. 生成分区健康监控任务数组和计数 (64字节数组 + 4字节count)
        generatePartitionHMTaskSection(module, fieldMap, baos, cpmStartAddress, partitionHealthMonitorConfigOffset);
        // 最后一个section后也要确保8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "PartitionHMTask_Padding");
        
        return baos.toByteArray();
    }

    /**
     * 生成核心健康监控表数组和配置个数
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
            
            byte[] addressBytes = BinaryUtils.longTo8BytesBigEndian(tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_CoreHMTable_" + i, addressBytes);
        }
        
        // 核心健康监控表配置个数 (4字节)
        byte[] coreHMTableCountBytes = BinaryUtils.longTo4BytesBigEndian(coreHMTableCount);
        baos.write(coreHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_CoreHMTableCount", coreHMTableCountBytes);
    }

    /**
     * 生成多分区健康监控表数组和配置个数
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
            
            byte[] addressBytes = BinaryUtils.longTo8BytesBigEndian(tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_MultiPartitionHMTable_" + i, addressBytes);
        }
        
        // 多分区健康监控表配置个数 (4字节)
        byte[] multiPartitionHMTableCountBytes = BinaryUtils.longTo4BytesBigEndian(multiPartitionHMTableCount);
        baos.write(multiPartitionHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_MultiPartitionHMTableCount", multiPartitionHMTableCountBytes);
    }

    /**
     * 生成分区健康监控表数组和配置个数
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
            
            byte[] addressBytes = BinaryUtils.longTo8BytesBigEndian(tableAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_PartitionHMTable_" + i, addressBytes);
        }
        
        // 分区健康监控表配置个数 (4字节)
        byte[] partitionHMTableCountBytes = BinaryUtils.longTo4BytesBigEndian(partitionHMTableCount);
        baos.write(partitionHMTableCountBytes);
        fieldMap.put("HealthMonitorConfig_PartitionHMTableCount", partitionHMTableCountBytes);
    }

    /**
     * 生成核心健康监控任务数组和配置个数
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
            
            byte[] addressBytes = BinaryUtils.longTo8BytesBigEndian(taskAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_CoreHMTask_" + i, addressBytes);
        }
        
        // 核心健康监控任务配置个数 (4字节)
        byte[] coreHMTaskCountBytes = BinaryUtils.longTo4BytesBigEndian(coreHMTaskCount);
        baos.write(coreHMTaskCountBytes);
        fieldMap.put("HealthMonitorConfig_CoreHMTaskCount", coreHMTaskCountBytes);
    }

    /**
     * 生成分区健康监控任务数组和配置个数
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
                int singleTaskSize = calculateSinglePartitionHMTaskSize();
                taskAddress = cpmStartAddress + taskConfigOffset + (i * singleTaskSize);
            } else {
                // 未配置的任务设置为0
                taskAddress = 0L;
            }
            
            byte[] addressBytes = BinaryUtils.longTo8BytesBigEndian(taskAddress);
            baos.write(addressBytes);
            fieldMap.put("HealthMonitorConfig_PartitionHMTask_" + i, addressBytes);
        }
        
        // 分区健康监控任务配置个数 (4字节)
        byte[] partitionHMTaskCountBytes = BinaryUtils.longTo4BytesBigEndian(partitionHMTaskCount);
        baos.write(partitionHMTaskCountBytes);
        fieldMap.put("HealthMonitorConfig_PartitionHMTaskCount", partitionHMTaskCountBytes);
    }

    /**
     * 计算健康监控配置存储结构的总大小
     * 根据64位规范和8字节对齐要求：
     *
     * 每组地址数组后跟4字节count，并将该组结构按8字节对齐。
     *
     * @param module 模块对象
     * @return 健康监控配置存储结构的大小（字节）
     */
    public static int calculateSize(Module module) {
        return calculateSectionSize(CORE_HM_TABLE_ARRAY_SIZE)
                + calculateSectionSize(MULTI_PARTITION_HM_TABLE_ARRAY_SIZE)
                + calculateSectionSize(PARTITION_HM_TABLE_ARRAY_SIZE)
                + calculateSectionSize(CORE_HM_TASK_ARRAY_SIZE)
                + calculateSectionSize(PARTITION_HM_TASK_ARRAY_SIZE);
    }

    /**
     * calculateSectionSize
     *
     * @brief
     * 功能: 执行 calculateSectionSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSectionSize(int arraySize) {
        int size = arraySize * 8 + 4;
        return ((size + 7) / 8) * 8;
    }

    /**
     * 在8字节字段前检查并添加对齐
     * 如果当前位置不是8字节对齐，就添加4字节padding
     * 
     * @param baos 字节输出流
     * @param fieldMap 字段映射表
     * @param paddingName padding字段的名称
     * @throws IOException 写入异常
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
     * 计算单个核心健康监控表结构的大小
     * 使用 CoreHMTableGenerator64big 计算实际的表结构大小
     */
    private static int calculateSingleCoreHMTableSize() {
        return CoreHMTableGenerator64big.calculateSingleTableSize();
    }

    /**
     * 计算单个多分区健康监控表结构的大小
     * 使用 MultiPartitionHMTableGenerator64big 计算实际的表结构大小
     */
    private static int calculateSingleMultiPartitionHMTableSize() {
        return MultiPartitionHMTableGenerator64big.calculateSingleTableSize();
    }

    /**
     * 计算单个分区健康监控表结构的大小
     * 使用 PartitionHealthMonitorTableConfigGenerator64big 计算实际的表结构大小
     */
    private static int calculateSinglePartitionHMTableSize() {
        return PartitionHealthMonitorTableConfigGenerator64big.calculateSingleTableConfigSize();
    }

    /**
     * 计算单个核心健康监控任务结构的大小
     * 使用 CoreHMTaskGenerator64big 计算实际的任务结构大小
     */
    private static int calculateSingleCoreHMTaskSize() {
        return CoreHMTaskGenerator64big.calculateSingleTaskSize();
    }

    /**
     * 计算单个分区健康监控任务结构的大小
     * 使用 PartitionHMTaskGenerator64big 计算实际的任务结构大小
     */
    private static int calculateSinglePartitionHMTaskSize() {
        return PartitionHMTaskGenerator64big.calculateSinglePartitionHMTaskSize();
    }
}
