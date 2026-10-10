/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: OsConfigGenerator32big.java
 * 
 * 描述    : 生成OsConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.CoreOS.HardwareConfiguration;
import com.nwpu.verify.entity.CoreOS.HardwareConfiguration.PhysicalMemory;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;
import com.nwpu.verify.layers.generation.utils.CRC32Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @brief
 * 功能: 操作系统配置二进制生成器 (32位大端)该类负责将从XML解析出的Module对象，按照预定格式生成二进制内容。。
 */
public class OsConfigGenerator32big {

    /**
     * generateBin
     *
     * @brief
     * 功能: 生成OS配置的二进制内容。。
     *
     * @param[in]  module:   从XML解析层传入的顶层Module对象。
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射，供对比层使用。
     * @return 生成的完整二进制字节数组。
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateBin(Module module, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // =======================================================================
            // 基础配置计算：CPM配置信息可加载文件起始地址，各个配置结构的大小，各个结构在文件中的偏移量
            // =======================================================================

            // 计算 "CPM配置信息可加载文件起始地址"
            HardwareConfiguration hardwareConfig = module.getfCoreOS().getfHardwareConfiguration();
            PhysicalMemory physicalMemory = hardwareConfig.getfPhysicalMemory();

            long baseAddress = physicalMemory.getfBaseAddress();
            long kernelMemoryRegionSize = physicalMemory.getfKernelMemoryRegionSize();
            long cpmStartAddress = baseAddress + kernelMemoryRegionSize;

            // 1.2 预计算后续各个配置结构的大小
            int headerSize = 5 * 4 + 4; // 32位系统：文件头包含5个unsigned long指针（每个4字节）+ 4字节对齐填充，共24字节

            int portConfigSize = PortConfigGenerator32big.calculateSize(module); // 端口配置结构大小
            int logConfigSize = calculateLogConfigSize(module); // 日志记事本配置结构大小
            int mainFrameConfigSize = calculateMainFrameConfigSize(module); // 主时间框架配置结构大小
            int scheduleConfigSize = calculateScheduleConfigSize(module); // 调度表配置结构大小
            int CoreHMTaskConfigSize = calculateCoreHMTaskConfigSize(module); // 核心健康监控任务配置结构大小
            int partitionHealthMonitorConfigSize = calculatePartitionHMConfigSize(module); // 分区健康监控任务配置结构大小
            int coreDispatchTableConfigSize = calculateCoreDispatchTableConfigSize(module); // 核心分派表配置结构大小
            int multiPartitionDispatchTableConfigSize = calculateMultiPartitionDispatchTableConfigSize(module); // 多分区分配表配置结构大小
            int partitionDispatchTableConfigSize = calculatePartitionDispatchTableConfigSize(module); // 分区分配表配置结构大小
            int coreHealthMonitorTableConfigSize = calculateCoreHealthMonitorTableConfigSize(module); // 核心健康监控表配置结构大小
            int multiPartitionHealthMonitorTableConfigSize = calculateMultiPartitionHealthMonitorTableConfigSize(
                    module); // 多分区健康监控表配置结构大小
            int partitionHealthMonitorTableConfigSize = calculatePartitionHealthMonitorTableConfigSize(module); // 分区健康监控表配置结构大小
            int healthMonitorConfigSize = calculateHealthMonitorConfigSize(module); // 健康监控表配置结构大小
            int sharedDataRegionPartitionConfigSize = calculateSharedDataRegionPartitionConfigSize(module); // 共享数据区分区配置结构大小
            int ppsScheduleConfigSize = calculatePpsScheduleConfigSize(module); // PPS调度配置存储结构大小
            int partitionConfigSize = calculatePartitionConfigSize(module); // 分区配置存储结构大小
            int sharedDataRegionConfigSize = calculateSharedDataRegionConfigSize(module); // 共享数据区配置存储结构大小
            int coreOsConfigSize = calculateCoreOsConfigSize(module); // 核心OS配置存储结构大小
            int memoryBlockConfigSize = calculateMemoryBlockConfigSize(module); // 内存块空间配置存储结构大小
            int storageRegionConfigSize = StorageRegionConfigGenerator32big.calculateSize(module); // 存储区配置存储结构大小
            int memoryManagementEntryConfigSize = calculateMemoryManagementEntryConfigSize(module); // 内存管理入口定义存储结构大小
            int spaceDescriptorConfigSize = calculateSpaceDescriptorConfigSize(module); // 空间域描述信息存储结构大小

            // 1.3 计算各个结构在文件中的偏移量
            long portConfigOffset = headerSize;
            long logConfigOffset = portConfigOffset + portConfigSize;
            long mainFrameConfigOffset = logConfigOffset + logConfigSize;
            long scheduleConfigOffset = mainFrameConfigOffset + mainFrameConfigSize;
            long CoreHMTaskConfigOffset = scheduleConfigOffset + scheduleConfigSize;
            long partitionHealthMonitorConfigOffset = CoreHMTaskConfigOffset + CoreHMTaskConfigSize;
            long rawCoreDispatchTableConfigOffset = partitionHealthMonitorConfigOffset
                    + partitionHealthMonitorConfigSize;
            // 三类分派表配置结构整体按8字节边界开始，对齐填充位于分派表整体之前。
            long coreDispatchTableConfigOffset = (rawCoreDispatchTableConfigOffset + 7) & ~7;
            long multiPartitionDispatchTableConfigOffset = coreDispatchTableConfigOffset + coreDispatchTableConfigSize;
            long partitionDispatchTableConfigOffset = multiPartitionDispatchTableConfigOffset
                    + multiPartitionDispatchTableConfigSize;
            // 分区分配表后需要8字节对齐，对齐填充计入偏移
            long partitionDispatchTableEnd = partitionDispatchTableConfigOffset + partitionDispatchTableConfigSize;
            long alignedPartitionDispatchTableEnd = (partitionDispatchTableEnd + 7) & ~7;
            long coreHealthMonitorTableConfigOffset = alignedPartitionDispatchTableEnd;
            long multiPartitionHealthMonitorTableConfigOffset = coreHealthMonitorTableConfigOffset
                    + coreHealthMonitorTableConfigSize;
            long partitionHealthMonitorTableConfigOffset = multiPartitionHealthMonitorTableConfigOffset
                    + multiPartitionHealthMonitorTableConfigSize;
            long healthMonitorConfigOffset = partitionHealthMonitorTableConfigOffset
                    + partitionHealthMonitorTableConfigSize;
            long sharedDataRegionPartitionConfigOffset = healthMonitorConfigOffset + healthMonitorConfigSize;
            long ppsScheduleConfigOffset = sharedDataRegionPartitionConfigOffset + sharedDataRegionPartitionConfigSize;
            long partitionConfigOffset = ppsScheduleConfigOffset + ppsScheduleConfigSize;
            long sharedDataRegionConfigOffset = partitionConfigOffset + partitionConfigSize;
            long coreOsConfigOffset = sharedDataRegionConfigOffset + sharedDataRegionConfigSize;
            long memoryBlockOffset = coreOsConfigOffset + coreOsConfigSize;
            long storageRegionOffset = memoryBlockOffset + memoryBlockConfigSize;
            long memoryManagementEntryOffset = storageRegionOffset + storageRegionConfigSize;
            long spaceDescriptorOffset = memoryManagementEntryOffset + memoryManagementEntryConfigSize;
            long bootInfoOffset = spaceDescriptorOffset + spaceDescriptorConfigSize;

            // =======================================================================
            // 按顺序写入位置信息
            // 32位系统中 unsigned long 占用4字节，共5个指针字段，总计 20 字节（参考数据词典表3）
            // =======================================================================

            // 字段 1: 核心OS配置存储结构位置 (unsigned long, 32位=4字节)
            // 计算方式: CPM起始地址 + 核心OS配置在文件中的偏移
            long coreOsConfigLocation = cpmStartAddress + coreOsConfigOffset;
            byte[] coreOsLocBytes = BinaryUtils.longTo4BytesBigEndian(coreOsConfigLocation);
            baos.write(coreOsLocBytes);
            fieldMap.put("CoreOSConfigLocation", coreOsLocBytes);

            // 字段 2: 引导信息表配置存储结构位置 (unsigned long, 32位=4字节)
            // 计算方式: CPM配置信息可加载文件起始地址+引导信息表配置存储结构在CPM配置信息可加载文件中的偏移。如未使用，预留位置填固定值 0
            long bootInfoLocation = cpmStartAddress + bootInfoOffset;
            byte[] bootInfoLocBytes = BinaryUtils.longTo4BytesBigEndian(bootInfoLocation);
            baos.write(bootInfoLocBytes);
            fieldMap.put("BootInfoTableLocation", bootInfoLocBytes);

            // 字段 3: 内存块空间配置存储结构位置 (unsigned long, 32位=4字节)
            // 计算方式: CPM起始地址 + 内存块配置在文件中的偏移
            long memoryBlockLocation = cpmStartAddress + memoryBlockOffset;
            byte[] memBlockLocBytes = BinaryUtils.longTo4BytesBigEndian(memoryBlockLocation);
            baos.write(memBlockLocBytes);
            fieldMap.put("MemoryBlockConfigLocation", memBlockLocBytes);

            // 字段 4: 存储区配置存储结构位置 (unsigned long, 32位=4字节)
            // 计算方式: CPM起始地址 + 存储区配置在文件中的偏移
            long storageRegionLocation = cpmStartAddress + storageRegionOffset;
            byte[] storageRegionLocBytes = BinaryUtils.longTo4BytesBigEndian(storageRegionLocation);
            baos.write(storageRegionLocBytes);
            fieldMap.put("StorageRegionConfigLocation", storageRegionLocBytes);

            // 字段 5: CPM配置信息可加载文件起始地址 (unsigned long, 32位=4字节)
            // 计算方式: 直接使用步骤1.1中计算出的值
            byte[] cpmStartAddrBytes = BinaryUtils.longTo4BytesBigEndian(cpmStartAddress);
            baos.write(cpmStartAddrBytes);
            fieldMap.put("CPMConfigStartAddress", cpmStartAddrBytes);

            // 字段 6: 文件头8字节对齐填充 (4字节)
            // 文件头5个字段共20字节，需要填充4字节对齐到24字节(8的倍数)
            byte[] headerPaddingBytes = new byte[4];
            baos.write(headerPaddingBytes);
            fieldMap.put("HeaderPadding", headerPaddingBytes);

            // =======================================================================
            // 后续各个模块的写入
            // =======================================================================

            // 端口配置数据存储结构
            byte[] portConfigBytes = PortConfigGenerator32big.generate(module, fieldMap);
            baos.write(portConfigBytes);

            // 日志记事本配置数据结构
            LogBookGenerator32big logBookGenerator = new LogBookGenerator32big();
            byte[] logBookConfigBytes = logBookGenerator.generateLogBookStruct(module, fieldMap);
            baos.write(logBookConfigBytes);

            // 主时间框架配置数据结构
            byte[] mainTimeFrameConfigBytes = MainTimeFrameGenerator32big.generate(module, fieldMap);
            // 验证实际生成大小与计算大小是否一致
            if (mainTimeFrameConfigBytes.length != mainFrameConfigSize) {
                System.err.println(
                        "警告: 主时间框架配置大小不匹配！计算值=" + mainFrameConfigSize + ", 实际值=" + mainTimeFrameConfigBytes.length
                                + ", 差异=" + (mainTimeFrameConfigBytes.length - mainFrameConfigSize));
            }
            baos.write(mainTimeFrameConfigBytes);

            // 调度表配置数据结构
            byte[] schedulesBytes = ScheduleConfigGenerator32big.generate(module, fieldMap, cpmStartAddress,
                    mainFrameConfigOffset);
            // 验证实际生成大小与计算大小是否一致
            if (schedulesBytes.length != scheduleConfigSize) {
                System.err.println("警告: 调度表配置大小不匹配！计算值=" + scheduleConfigSize + ", 实际值=" + schedulesBytes.length
                        + ", 差异=" + (schedulesBytes.length - scheduleConfigSize));
            }
            baos.write(schedulesBytes);

            // 核心健康监控任务配置数据结构
            byte[] coreHMTaskConfigBytes = CoreHMTaskGenerator32big.generateCoreHMTaskConfigs(module, fieldMap);
            baos.write(coreHMTaskConfigBytes);

            // 分区健康监控任务配置数据结构
            PartitionHMTaskGenerator32big hmTaskGenerator = new PartitionHMTaskGenerator32big();
            byte[] partitionHmTaskConfigBytes = hmTaskGenerator.generatePartitionHMTasksBin(module, fieldMap);
            baos.write(partitionHmTaskConfigBytes);

            // 核心分派表配置数据结构整体按8字节边界开始
            int coreDispatchTableAlignmentPadding = (int) (coreDispatchTableConfigOffset
                    - rawCoreDispatchTableConfigOffset);
            if (coreDispatchTableAlignmentPadding > 0) {
                byte[] alignmentPadding = new byte[coreDispatchTableAlignmentPadding];
                baos.write(alignmentPadding);
                fieldMap.put("CoreDispatchTable_AlignmentPadding", alignmentPadding);
            }

            // 核心分派表配置数据结构
            byte[] coreDispatchTableConfigBytes = CoreDispatchTableConfigGenerator32big.generate(module, fieldMap);
            baos.write(coreDispatchTableConfigBytes);

            // 多分区分配表配置数据结构
            byte[] multiPartitionDispatchTableConfigBytes = MultiPartitionDispatchTableConfigGenerator32big
                    .generate(module, fieldMap);
            baos.write(multiPartitionDispatchTableConfigBytes);

            // 分区分配表配置数据结构
            byte[] partitionDispatchTableConfigBytes = PartitionDispatchTableGenerator32big
                    .generatePartitionDispatchTables(module, fieldMap);
            baos.write(partitionDispatchTableConfigBytes);

            // 分派表配置数据结构整体结束后按8字节边界对齐，保证后续核心健康监控表起始地址正确
            {
                int currentSize = baos.size();
                int alignedSize = (currentSize + 7) & ~7;
                int paddingSize = alignedSize - currentSize;
                if (paddingSize > 0) {
                    byte[] alignmentPadding = new byte[paddingSize];
                    baos.write(alignmentPadding);
                    fieldMap.put("PartitionDispatchTable_AlignmentPadding", alignmentPadding);
                }
            }

            // 核心健康监控表配置数据结构
            byte[] coreHealthMonitorTableConfigBytes = CoreHMTableGenerator32big.generateCoreHMTableConfigs(
                    module, fieldMap, cpmStartAddress, coreDispatchTableConfigOffset);
            baos.write(coreHealthMonitorTableConfigBytes);

            // 多分区健康监控表配置数据结构
            MultiPartitionHMTableGenerator32big multiPartitionHMTableGenerator = new MultiPartitionHMTableGenerator32big();
            byte[] multiPartitionHealthMonitorTableConfigBytes = multiPartitionHMTableGenerator
                    .generateMultiPartitionHMTables(
                            module, fieldMap, cpmStartAddress, multiPartitionDispatchTableConfigOffset);
            baos.write(multiPartitionHealthMonitorTableConfigBytes);

            // 分区健康监控表配置数据结构
            byte[] partitionHealthMonitorTableConfigBytes = PartitionHealthMonitorTableConfigGenerator32big.generate(
                    module, fieldMap, cpmStartAddress, partitionDispatchTableConfigOffset);
            baos.write(partitionHealthMonitorTableConfigBytes);

            // 健康监控配置数据结构
            byte[] healthMonitorConfigBytes = HealthMonitorConfigGenerator32big.generate(module, fieldMap,
                    cpmStartAddress,
                    coreHealthMonitorTableConfigOffset, multiPartitionHealthMonitorTableConfigOffset,
                    partitionHealthMonitorTableConfigOffset, CoreHMTaskConfigOffset,
                    partitionHealthMonitorConfigOffset);
            baos.write(healthMonitorConfigBytes);

            // 共享数据区分区配置数据结构
            SharedDataRegionNameGenerator32big sharedDataRegionNameGenerator = new SharedDataRegionNameGenerator32big();
            byte[] sharedDataRegionPartitionConfigBytes = sharedDataRegionNameGenerator
                    .generateSharedDataRegionNamesBin(module, fieldMap);
            baos.write(sharedDataRegionPartitionConfigBytes);

            // PPS调度配置数据结构
            byte[] ppsConfigBytes = PPSConfigGenerator32big.generatePPSConfigs(module, fieldMap);
            baos.write(ppsConfigBytes);

            // 分区配置数据结构
            PartitionConfigGenerator32big partitionConfigGenerator = new PartitionConfigGenerator32big();
            byte[] partitionConfigBytes = partitionConfigGenerator.generatePartitionConfigStruct(module, fieldMap,
                    cpmStartAddress, logConfigOffset, sharedDataRegionPartitionConfigOffset);
            baos.write(partitionConfigBytes);

            // 共享数据区配置数据结构
            java.util.List<Byte> sharedDataRegionConfigList = SharedDataRegionGenerator32big
                    .generateSharedDataRegionConfigs(module, fieldMap);
            ByteArrayOutputStream sharedDataStream = new ByteArrayOutputStream();
            for (Byte b : sharedDataRegionConfigList) {
                sharedDataStream.write(b);
            }
            byte[] sharedDataRegionConfigBytes = sharedDataStream.toByteArray();
            baos.write(sharedDataRegionConfigBytes);

            // 核心OS配置数据结构
            // 构造所需的调度表偏移映射 Map<Integer,Long>
            Map<Integer, Long> scheduleConfigOffsets = buildScheduleConfigOffsets(module, scheduleConfigOffset);

            // 构造所需的分区偏移映射 Map<String,Long>
            Map<String, Long> partitionConfigOffsets = buildPartitionConfigOffsets(module, partitionConfigOffset);

            byte[] coreOSConfigBytes = CoreOSConfigGenerator32big.generate(
                    module,
                    fieldMap,
                    cpmStartAddress,
                    healthMonitorConfigOffset,
                    scheduleConfigOffsets,
                    partitionConfigOffsets,
                    portConfigOffset,
                    sharedDataRegionConfigOffset);
            // 验证实际生成大小与计算大小是否一致
            if (coreOSConfigBytes.length != coreOsConfigSize) {
                System.err.println("警告: 核心OS配置大小不匹配！计算值=" + coreOsConfigSize + ", 实际值=" + coreOSConfigBytes.length
                        + ", 差异=" + (coreOSConfigBytes.length - coreOsConfigSize));
            }
            baos.write(coreOSConfigBytes);

            // 内存块空间配置数据结构
            MemoryBlockConfigMasterGenerator32big memoryBlockConfigGenerator = new MemoryBlockConfigMasterGenerator32big();
            byte[] memoryBlockConfigBytes = memoryBlockConfigGenerator.generateAllMemoryBlockConfigs(module, fieldMap);
            baos.write(memoryBlockConfigBytes);

            // 存储区配置数据结构
            byte[] storageRegionConfigBytes = StorageRegionConfigGenerator32big.generate(module, fieldMap);
            baos.write(storageRegionConfigBytes);

            // 内存管理入口定义数据结构
            byte[] memoryManagerEntryBytes = MemoryManagerEntryGenerator32big.generateMemoryManagerEntries(module,
                    fieldMap);
            baos.write(memoryManagerEntryBytes);

            // 空间域描述信息数据结构
            SpaceDomainDescriptorGenerator32big spaceDomainDescriptorGenerator = new SpaceDomainDescriptorGenerator32big();
            byte[] spaceDomainDescriptorBytes = spaceDomainDescriptorGenerator.generateAllSpaceDomainDescriptors(module,
                    fieldMap);
            baos.write(spaceDomainDescriptorBytes);

            // 引导信息表配置数据结构
            byte[] bootInfoTableBytes = BootInfoTableGenerator32big.generateBootInfoTable(
                    module, fieldMap, cpmStartAddress, spaceDescriptorOffset, memoryManagementEntryOffset);
            baos.write(bootInfoTableBytes);

            // 填充处理
            // 填充0，直至整个CPM配置信息可加载文件的大小与kernelConfigRecordRegion/@Size属性值保持一致
            byte[] dataBeforePadding = baos.toByteArray();
            int currentSize = dataBeforePadding.length;

            // 获取目标大小：kernelConfigRecordRegion/@Size（不包括CRC32的4字节）
            String kernelConfigRecordRegionSizeStr = physicalMemory.getfKernelConfigRecordRegionSize();
            long kernelConfigRecordRegionSize = 0;

            if (kernelConfigRecordRegionSizeStr != null && !kernelConfigRecordRegionSizeStr.isEmpty()) {
                try {
                    // 支持十六进制和十进制格式
                    if (kernelConfigRecordRegionSizeStr.startsWith("0x")
                            || kernelConfigRecordRegionSizeStr.startsWith("0X")) {
                        // 十六进制格式：0x100000
                        String hexStr = kernelConfigRecordRegionSizeStr.substring(2); // 去掉"0x"前缀
                        kernelConfigRecordRegionSize = Long.parseLong(hexStr, 16);
                    } else {
                        // 十进制格式：1048576
                        kernelConfigRecordRegionSize = Long.parseLong(kernelConfigRecordRegionSizeStr);
                    }
                    System.out.println("解析kernelConfigRecordRegionSize: " + kernelConfigRecordRegionSizeStr + " -> "
                            + kernelConfigRecordRegionSize + " 字节");
                } catch (NumberFormatException e) {
                    System.err.println("警告: 无法解析kernelConfigRecordRegionSize: " + kernelConfigRecordRegionSizeStr);
                    kernelConfigRecordRegionSize = currentSize + 4; // 默认值：当前大小+CRC32
                }
            } else {
                System.err.println("警告: kernelConfigRecordRegionSize为空，使用默认值");
                kernelConfigRecordRegionSize = currentSize + 4; // 默认值：当前大小+CRC32
            }

            long targetSizeWithoutCRC = kernelConfigRecordRegionSize - 4; // 减去CRC32的4字节

            // 计算需要填充的字节数
            long paddingSize = targetSizeWithoutCRC - currentSize;

            if (paddingSize > 0) {
                // 添加填充字节（全部为0）
                byte[] paddingBytes = new byte[(int) paddingSize];
                // Java中byte数组默认初始化为0，所以不需要额外设置
                baos.write(paddingBytes);
                // 移除 PaddingBytes 字段，不将其添加到 fieldMap 中

                System.out.println("已添加填充: " + paddingSize + " 字节，目标大小: " + kernelConfigRecordRegionSize);
            } else if (paddingSize < 0) {
                System.err.println("警告: 当前数据大小(" + currentSize + ")超过了目标大小(" + targetSizeWithoutCRC + ")");
            } else {
                System.out.println("无需填充，当前大小正好匹配目标大小");
            }

            // CRC32校验码
            byte[] dataWithoutCRC = baos.toByteArray();

            // 计算CRC32校验码
            long crc32Value = CRC32Utils.calculateCRC32(dataWithoutCRC);
            byte[] crc32Bytes = CRC32Utils.crc32ToBytes(crc32Value);

            // 写入CRC32校验码
            baos.write(crc32Bytes);

            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // =======================================================================
    // 以下是各个存储结构位置占用字节数计算函数
    // =======================================================================

    /**
     * calculateLogConfigSize
     *
     * @brief
     * 功能: 执行 calculateLogConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateLogConfigSize(Module module) {
        // 使用 LogBookGenerator32big 计算日志记事本配置的总大小
        // 每个LogBook结构固定48字节：fLogbookName(32) + fMaxMessageSize(4) +
        // fMaxNBLoggedMessages(4) + fMaxNBInProgressMessages(4) + fSerialNumber(4)
        // 如果配置中没有LogBook，返回0是正常的
        LogBookGenerator32big logBookGenerator = new LogBookGenerator32big();
        return logBookGenerator.calculateLogBookStructSize(module);
    }

    /**
     * calculateMainFrameConfigSize
     *
     * @brief
     * 功能: 执行 calculateMainFrameConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateMainFrameConfigSize(Module module) {
        // 使用 MainTimeFrameGenerator32big 计算主时间框架配置的总大小
        return MainTimeFrameGenerator32big.calculateSize(module);
    }

    /**
     * calculateScheduleConfigSize
     *
     * @brief
     * 功能: 执行 calculateScheduleConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateScheduleConfigSize(Module module) {
        return ScheduleConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculateCoreHMTaskConfigSize
     *
     * @brief
     * 功能: 执行 calculateCoreHMTaskConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateCoreHMTaskConfigSize(Module module) {
        return CoreHMTaskGenerator32big.calculateSize(module);
    }

    /**
     * calculatePartitionHMConfigSize
     *
     * @brief
     * 功能: 执行 calculatePartitionHMConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculatePartitionHMConfigSize(Module module) {
        // 使用 PartitionHMTaskGenerator32big 计算分区健康监控任务配置的总大小
        int singleTaskSize = PartitionHMTaskGenerator32big.calculateSinglePartitionHMTaskSize(); // 1132

        // 获取分区健康监控任务数量
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getfPartitionHMTasks() == null) {
            return 0;
        }

        int taskCount = module.getfHealthMonitor().getfPartitionHMTasks().size();
        if (taskCount <= 0) return 0;
        // 非最后一个任务后有8字节对齐填充（1132 + 4 = 1136），最后一个任务后无填充
        int alignedSingleTaskSize = (singleTaskSize + 7) & ~7; // 1136
        return (taskCount - 1) * alignedSingleTaskSize + singleTaskSize;
    }

    /**
     * calculateCoreDispatchTableConfigSize
     *
     * @brief
     * 功能: 执行 calculateCoreDispatchTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateCoreDispatchTableConfigSize(Module module) {
        return CoreDispatchTableConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculateMultiPartitionDispatchTableConfigSize
     *
     * @brief
     * 功能: 执行 calculateMultiPartitionDispatchTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateMultiPartitionDispatchTableConfigSize(Module module) {
        return MultiPartitionDispatchTableConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculatePartitionDispatchTableConfigSize
     *
     * @brief
     * 功能: 执行 calculatePartitionDispatchTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculatePartitionDispatchTableConfigSize(Module module) {
        return PartitionDispatchTableGenerator32big.calculateSize(module);
    }

    /**
     * calculateCoreHealthMonitorTableConfigSize
     *
     * @brief
     * 功能: 执行 calculateCoreHealthMonitorTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateCoreHealthMonitorTableConfigSize(Module module) {
        return CoreHMTableGenerator32big.calculateSize(module);
    }

    /**
     * calculateMultiPartitionHealthMonitorTableConfigSize
     *
     * @brief
     * 功能: 执行 calculateMultiPartitionHealthMonitorTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateMultiPartitionHealthMonitorTableConfigSize(Module module) {
        return MultiPartitionHMTableGenerator32big.calculateSize(module);
    }

    /**
     * calculatePartitionHealthMonitorTableConfigSize
     *
     * @brief
     * 功能: 执行 calculatePartitionHealthMonitorTableConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculatePartitionHealthMonitorTableConfigSize(Module module) {
        return PartitionHealthMonitorTableConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculateHealthMonitorConfigSize
     *
     * @brief
     * 功能: 执行 calculateHealthMonitorConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateHealthMonitorConfigSize(Module module) {
        return HealthMonitorConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculateSharedDataRegionPartitionConfigSize
     *
     * @brief
     * 功能: 执行 calculateSharedDataRegionPartitionConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateSharedDataRegionPartitionConfigSize(Module module) {
        // 使用 SharedDataRegionNameGenerator32big 计算共享数据区分区配置的总大小
        // 这是指共享数据区分区配置存储结构，即共享数据区名配置
        SharedDataRegionNameGenerator32big sharedDataRegionNameGenerator = new SharedDataRegionNameGenerator32big();
        return sharedDataRegionNameGenerator.calculateSharedDataRegionNmaeStructSize(module);
    }

    /**
     * calculatePpsScheduleConfigSize
     *
     * @brief
     * 功能: 执行 calculatePpsScheduleConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculatePpsScheduleConfigSize(Module module) {
        return PPSConfigGenerator32big.calculateSize(module);
    }

    /**
     * calculatePartitionConfigSize
     *
     * @brief
     * 功能: 执行 calculatePartitionConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculatePartitionConfigSize(Module module) {
        if (module.getfPartitions() == null) {
            return 0;
        }

        // 只计算真实分区的大小，排除虚分区
        int realPartitionCount = 0;
        for (String partitionName : module.getfPartitions().keySet()) {
            com.nwpu.verify.entity.Partition partition = module.getfPartitions().get(partitionName);
            if (!partition.isFIsPseudoPartition()) {
                realPartitionCount++;
            }
        }

        PartitionConfigGenerator32big partitionGenerator = new PartitionConfigGenerator32big();
        int singlePartitionSize = partitionGenerator.calculateSinglePartitionConfigSize();
        return singlePartitionSize * realPartitionCount;
    }

    /**
     * calculateSharedDataRegionConfigSize
     *
     * @brief
     * 功能: 执行 calculateSharedDataRegionConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateSharedDataRegionConfigSize(Module module) {
        return SharedDataRegionGenerator32big.calculateTotalSize(module);
    }

    /**
     * calculateCoreOsConfigSize
     *
     * @brief
     * 功能: 执行 calculateCoreOsConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateCoreOsConfigSize(Module module) {
        return CoreOSConfigGenerator32big.calculateStructSize();
    }

    /**
     * calculateMemoryBlockConfigSize
     *
     * @brief
     * 功能: 执行 calculateMemoryBlockConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateMemoryBlockConfigSize(Module module) {
        // 内存块配置包含23个内存块空间配置存储结构
        // 使用静态方法计算大小，避免重复生成二进制内容
        return MemoryBlockConfigMasterGenerator32big.calculateSize(module);
    }

    /**
     * calculateMemoryManagementEntryConfigSize
     *
     * @brief
     * 功能: 执行 calculateMemoryManagementEntryConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateMemoryManagementEntryConfigSize(Module module) {
        return MemoryManagerEntryGenerator32big.calculateSize(module);
    }

    /**
     * calculateSpaceDescriptorConfigSize
     *
     * @brief
     * 功能: 执行 calculateSpaceDescriptorConfigSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int calculateSpaceDescriptorConfigSize(Module module) {
        // 空间域描述信息包含核心OS、共享库、分区的空间域描述信息
        // 每个结构的大小需要根据具体实现来计算
        try {
            SpaceDomainDescriptorGenerator32big generator = new SpaceDomainDescriptorGenerator32big();
            byte[] tempBytes = generator.generateAllSpaceDomainDescriptors(module, new java.util.HashMap<>());
            return tempBytes.length;
        } catch (Exception e) {
            return 2048; // 2KB作为默认估算
        }
    }

    /**
     * buildScheduleConfigOffsets
     *
     * @brief
     * 功能: 构建调度表配置偏移映射。
     *
     * @param[in]  module:               Module对象，包含调度表信息
     * @param[in]  scheduleConfigOffset: 调度表配置在文件中的起始偏移
     * @return 调度表ID到偏移的映射
     * @scope internal
     * @errno:
     * N/A
     */
    private Map<Integer, Long> buildScheduleConfigOffsets(Module module, long scheduleConfigOffset) {
        Map<Integer, Long> scheduleOffsets = new HashMap<>();

        if (module.getfSchedules() != null && !module.getfSchedules().isEmpty()) {
            // 32位系统计算单个调度表结构的大小（参考数据词典表5）：
            // 配置类型(4) + 配置数据大小(4) + 调度表名称(32) + 主时间框架指针(4) + 时间窗口数目(4) + 处理器核号(4) +
            // 缺省调度表标志(4) = 56 bytes
            final int BASE_SCHEDULE_STRUCT_SIZE = 4 + 4 + 32 + 4 + 4 + 4 + 4;
            // 8字节对齐后的大小
            final int SINGLE_SCHEDULE_STRUCT_SIZE = (BASE_SCHEDULE_STRUCT_SIZE + 7) & ~7; // 56 bytes (已8字节对齐)

            long currentOffset = scheduleConfigOffset;

            // 按XML文件中的出现顺序处理（LinkedHashMap保持插入顺序）
            // 这与ScheduleConfigGenerator32big中的生成顺序保持一致
            for (com.nwpu.verify.entity.Schedule schedule : module.getfSchedules().values()) {
                if (schedule != null) {
                    int scheduleId = schedule.getScheduleIdentifier();
                    // 记录当前调度表的偏移，键为scheduleId
                    scheduleOffsets.put(scheduleId, currentOffset);
                    // 更新偏移量，使用实际的调度表大小
                    currentOffset += SINGLE_SCHEDULE_STRUCT_SIZE;
                }
            }
        }

        return scheduleOffsets;
    }

    /**
     * buildPartitionConfigOffsets
     *
     * @brief
     * 功能: 构建分区配置偏移映射（只包含真实分区，排除虚分区）。
     *
     * @param[in]  module:                Module对象，包含分区信息
     * @param[in]  partitionConfigOffset: 分区配置在文件中的起始偏移
     * @return 分区名到偏移的映射
     * @scope internal
     * @errno:
     * N/A
     */
    private Map<String, Long> buildPartitionConfigOffsets(Module module, long partitionConfigOffset) {
        Map<String, Long> partitionOffsets = new HashMap<>();

        if (module.getfPartitions() != null) {
            long currentOffset = partitionConfigOffset;

            // 计算单个分区配置的大小
            PartitionConfigGenerator32big partitionGenerator = new PartitionConfigGenerator32big();
            int singlePartitionSize = partitionGenerator.calculateSinglePartitionConfigSize();

            // 为每个真实分区分配偏移（跳过虚分区）
            for (String partitionName : module.getfPartitions().keySet()) {
                com.nwpu.verify.entity.Partition partition = module.getfPartitions().get(partitionName);

                // 跳过虚分区
                if (partition.isFIsPseudoPartition()) {
                    System.out.println("DEBUG: 跳过虚分区 '" + partitionName + "' - 不生成偏移映射");
                    continue;
                }

                partitionOffsets.put(partitionName, currentOffset);
                System.out.println("DEBUG: 真实分区 '" + partitionName + "' 偏移映射: " +
                        currentOffset + " (0x" + Long.toHexString(currentOffset) + ")");
                currentOffset += singlePartitionSize;
            }
        }

        return partitionOffsets;
    }

}
