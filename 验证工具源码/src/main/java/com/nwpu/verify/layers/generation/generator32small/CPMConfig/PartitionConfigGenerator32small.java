/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionConfigGenerator32small.java
 * 
 * 描述    : 生成PartitionConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.entity.LogBook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @brief
 * 功能: 分区配置二进制生成器 (32位大端)。
 */
public class PartitionConfigGenerator32small {

    // 辅助方法：long 4字节大端数组
    /**
     * longTo4BytesLittleEndian
     *
     * @brief
     * 功能: 执行 longTo4BytesLittleEndian 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] longTo4BytesLittleEndian(long value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
    }

    // 辅助方法：long 8字节大端数组
    /**
     * longTo8BytesLittleEndian
     *
     * @brief
     * 功能: 执行 longTo8BytesLittleEndian 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] longTo8BytesLittleEndian(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    // 辅助方法：int 4字节大端数组
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

    // 辅助方法：字符串转固定长度、\0填充的字节数组
    /**
     * fixedLengthAscii
     *
     * @brief
     * 功能: 执行 fixedLengthAscii 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] fixedLengthAscii(String str, int length) {
        byte[] bytes = new byte[length];
        if (str != null) {
            byte[] src = str.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, bytes, 0, Math.min(src.length, length));
        }
        return bytes;
    }

    /**
     * calculateSinglePartitionConfigSize
     *
     * @brief
     * 功能: 根据文档计算单个分区配置结构的大小（包含动态8字节对齐padding）模拟实际生成过程，动态计算是否需要padding32位系统版本 - unsigned long字段使用4字节。
     *
     * @return 单个 Partition 结构体在二进制文件中的总大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public int calculateSinglePartitionConfigSize() {
        int size = 0;

        // 字段1-5: 4+4+32+4+4 = 48
        size += 48;

        // 字段6-10: 4+4+8+8+8 = 32 (32位：分区逻辑空间地址4 + 分区逻辑空间大小4 + 看门狗时长8 + 分区周期8 + 分区持续时间8)
        size += 32;

        // 字段11-12: 4+4 = 8
        size += 8;

        // 字段13: 4 (32位：共享数据分区配置存储结构地址，unsigned long)
        size += 4;

        // 字段14: 4
        size += 4;
        // 检查是否需要padding（在字段15之前，字段15是unsigned long，32位4字节）
        // 当前size = 48+32+8+4+4 = 96，已8字节对齐，不需要padding

        // 字段15: 4 (32位：日志记事本配置存储结构地址，unsigned long)
        size += 4;

        // 字段16-17: 4+4 = 8
        size += 8;

        // 字段18-23: 4+4+4+4+4+4 = 24
        size += 24;
        // 检查是否需要padding（在字段24之前，字段24是unsigned long，32位4字节）
        // 当前size = 96+4+8+24 = 132，不需要padding（4字节对齐即可）

        // 字段24: 4 (32位：用户堆空间大小，unsigned long)
        size += 4;

        // 字段25-26: 4+4 = 8
        size += 8;
        // 检查是否需要padding（在字段27之前，字段27是signed long long，需要8字节对齐）
        size = addPaddingIfNeeded(size);

        // 字段27: 8 (signed long long)
        size += 8;

        // 字段28-30: 4+4+4 = 12
        size += 12;
        // 检查是否需要padding（在字段31之前，字段31是unsigned long，32位4字节）
        // 不需要特殊对齐

        // 字段31-34: 4+4+4+4 = 16 (32位：端口配置地址4 + 分区端口数4 + 系统分区标志4 + 分区允许切换调度表标志4)
        size += 16;

        // 字段35: 4
        size += 4;
        // 检查是否需要padding（在字段36之前，字段36是unsigned long，32位4字节）
        // 不需要特殊对齐

        // 字段36: 4 (32位：分区内设备配置地址，unsigned long)
        size += 4;

        // 字段37: 4
        size += 4;
        // 检查是否需要padding（在字段38之前，字段38是unsigned long，32位4字节）
        // 不需要特殊对齐

        // 字段38-41: 4+4+4+4 = 16 (32位：4个unsigned long字段)
        size += 16;

        // 字段42: 4
        size += 4;

        // 字段43: 32
        size += 32;

        // 字段44-46: 4+4+4 = 12
        size += 12;
        // 检查是否需要padding（在字段47之前，字段47是unsigned long，32位4字节）
        // 不需要特殊对齐

        // 字段47: 4 (32位：GOS分区配置地址，unsigned long)
        size += 4;

        // 字段48-49: 4+4 = 8
        size += 8;

        // 最后确保整个结构8字节对齐
        size = alignTo8Bytes(size);

        return size;
    }

    /**
     * addPaddingIfNeeded
     *
     * @brief
     * 功能: 辅助方法：检查当前大小是否需要padding来达到8字节对齐如果需要，返回对齐后的大小；否则返回原大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private int addPaddingIfNeeded(int currentSize) {
        if (currentSize % 8 != 0) {
            return alignTo8Bytes(currentSize);
        }
        return currentSize;
    }

    /**
     * generatePartitionConfigStruct
     *
     * @brief
     * 功能: 生成所有分区配置结构的二进制内容。
     *
     * @param[in]  module:                                包含所有配置信息的Module对象
     * @param[in]  fieldMap:                              用于记录每个字段名及其对应二进制内容的映射
     * @param[in]  cpmStartAddress:                       CPM配置信息可加载文件的起始地址
     * @param[in]  logConfigOffset:                       日志记事本配置存储结构在CPM配置信息可加载文件中的偏移
     * @param[in]  sharedDataRegionPartitionConfigOffset: 共享数据区分区配置存储结构在CPM配置信息可加载文件中的偏移
     * @return 所有分区配置结构的完整二进制字节数组
     * @throws IOException 写入流时可能发生异常
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generatePartitionConfigStruct(Module module, Map<String, byte[]> fieldMap, long cpmStartAddress,
            long logConfigOffset, long sharedDataRegionPartitionConfigOffset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int singlePartitionSize = calculateSinglePartitionConfigSize();

        for (Map.Entry<String, Partition> entry : module.getfPartitions().entrySet()) {
            String partitionNameKey = entry.getKey();
            Partition partition = entry.getValue();
            // 跳过虚分区
            if (partition.isFIsPseudoPartition()) {
                continue;
            }
            Partition.Settings settings = partition.getfSettings();

            // 1. 配置类型 (fConfigType) - 4字节, 固定值1129468930
            byte[] configTypeBytes = intToBytesLittleEndian(1129468930);
            baos.write(configTypeBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_ConfigType", configTypeBytes);

            // 2. 配置数据大小 (fConfigDataSize) - 4字节, 结构体总大小
            byte[] configSizeBytes = intToBytesLittleEndian(singlePartitionSize);
            baos.write(configSizeBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_ConfigDataSize", configSizeBytes);

            // 3. 分区名(fName) - 32字节, 不足\0填充
            byte[] nameBytes = fixedLengthAscii(partition.getfNameAsString(), 32);
            baos.write(nameBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_Name", nameBytes);

            // 4. 分区标识符(fIdentifier) - 4字节
            byte[] identifierBytes = intToBytesLittleEndian(partition.getfIdentifier());
            baos.write(identifierBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_Identifier", identifierBytes);

            // 5. 分区类型 (fPartitionType) - 4字节, 固定值0
            byte[] typeBytes = intToBytesLittleEndian(0);
            baos.write(typeBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_PartitionType", typeBytes);

            // 6. 分区逻辑空间地址 (fPartitionLogicalSpaceAddress) - 4字节 (32位，unsigned long)
            long partitionVirtualAddress = module.getfCoreOS().getfKernelConfiguration().getfPartitionVirtualAddress();
            byte[] logicalAddrBytes = longTo4BytesLittleEndian(partitionVirtualAddress);
            baos.write(logicalAddrBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_PartitionLogicalSpaceAddress", logicalAddrBytes);

            // 7. 分区逻辑空间大小 (fPartitionLogicalSpaceSize) - 4字节 (32位，unsigned long)
            long partitionSpaceSize = module.getfCoreOS().getfKernelConfiguration().getfPartitionSpaceSize();
            byte[] logicalSizeBytes = longTo4BytesLittleEndian(partitionSpaceSize);
            baos.write(logicalSizeBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_PartitionLogicalSpaceSize", logicalSizeBytes);

            // 8. 看门狗时长(fWatchDogDuration) - 8字节, 默认 1
            byte[] watchdogBytes = longTo8BytesLittleEndian(settings != null ? settings.getfWatchDogDuration() : 1L);
            baos.write(watchdogBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_WatchDogDuration", watchdogBytes);

            // 9. 分区周期 (fPeriod) - 8字节, 默认 2
            byte[] periodBytes = longTo8BytesLittleEndian(settings != null ? settings.getfPeriod() : 2L);
            baos.write(periodBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_Period", periodBytes);

            // 10. 分区持续时间 (fDuration) - 8字节, 默认 3
            byte[] durationBytes = longTo8BytesLittleEndian(settings != null ? settings.getfDuration() : 3L);
            baos.write(durationBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_Duration", durationBytes);

            // 11. 最大全局描述符个数(fMaxGlobalFDs) - 4字节, 默认 20
            int maxGlobalFDs = (settings != null) ? settings.getfMaxGlobalFDs() : 20;
            baos.write(intToBytesLittleEndian(maxGlobalFDs));
            fieldMap.put("Partition_" + partitionNameKey + "_MaxGlobalFDs", intToBytesLittleEndian(maxGlobalFDs));

            // 12. worker 任务数(fNumWorkerTasks) - 4字节, 默认 0
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfNumWorkerTasks() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_NumWorkerTasks",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfNumWorkerTasks() : 0));

            // 13. 共享数据分区配置存储结构地址 - 4字节 (32位，unsigned long),
            // CPM配置信息可加载文件起始地址+本分区所属的第一个共享数据分区配置存储结构在CPM配置信息可加载文件中的偏移
            // 如果该分区不存在共享数据区，默认为0
            int sharedDataRegionCount = (partition.getfSharedDataRegions() != null)
                    ? partition.getfSharedDataRegions().size()
                    : 0;
            long sharedDataRegionConfigAddress = 0L;
            if (sharedDataRegionCount > 0) {
                long partitionSharedDataOffset = calculatePartitionSharedDataRegionConfigOffset(module, partition,
                        sharedDataRegionPartitionConfigOffset);
                sharedDataRegionConfigAddress = cpmStartAddress + partitionSharedDataOffset;
            }
            baos.write(longTo4BytesLittleEndian(sharedDataRegionConfigAddress));
            fieldMap.put("Partition_" + partitionNameKey + "_SharedDataRegionConfigAddress",
                    longTo4BytesLittleEndian(sharedDataRegionConfigAddress));

            // 14. 共享数据区数 - 4字节（已在上面计算）
            baos.write(intToBytesLittleEndian(sharedDataRegionCount));
            fieldMap.put("Partition_" + partitionNameKey + "_SharedDataRegionCount",
                    intToBytesLittleEndian(sharedDataRegionCount));

            // 15. 日志记事本配置存储结构地址 - 4字节 (32位，unsigned long),
            // CPM配置信息可加载文件起始地址+本分区所属的日志记事本存储结构在CPM配置信息可加载文件中的偏移
            // 先获取日志记事本数量
            int logBookCount = getPartitionLogBookCount(module, partition);
            long logBookConfigAddress = 0L;
            if (logBookCount > 0) {
                long partitionLogBookOffset = calculatePartitionLogBookConfigOffset(module, partition, logConfigOffset);
                logBookConfigAddress = cpmStartAddress + partitionLogBookOffset;
            }
            baos.write(longTo4BytesLittleEndian(logBookConfigAddress));
            fieldMap.put("Partition_" + partitionNameKey + "_LogBookConfigAddress",
                    longTo4BytesLittleEndian(logBookConfigAddress));

            // 16. 日志记事本数 - 4字节（已在上面计算）
            baos.write(intToBytesLittleEndian(logBookCount));
            fieldMap.put("Partition_" + partitionNameKey + "_LogBookCount", intToBytesLittleEndian(logBookCount));

            // 17. 系统调用允许标志 (fSyscallPermissions) - 4字节, 默认 0xFFFFFFFF
            baos.write(longTo4BytesLittleEndian(settings != null ? settings.getfSyscallPermissions() : 0xFFFFFFFFL));
            fieldMap.put("Partition_" + partitionNameKey + "_SyscallPermissions",
                    longTo4BytesLittleEndian(settings != null ? settings.getfSyscallPermissions() : 0xFFFFFFFFL));

            // 18. 允许打开的最大文件数 (fNumfiles) - 4字节, 默认 0 [cite: 68]
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfNumFiles() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_NumFiles",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfNumFiles() : 0));

            // 19. 最大设备数 (fNumDrivers) - 4字节, 默认 0 [cite: 68]
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfNumDrivers() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_NumDrivers",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfNumDrivers() : 0));

            // 20. 最大日志消息数 (fNumLogMsgs) - 4字节, 默认 0 [cite: 68]
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfNumLogMsgs() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_NumLogMsgs",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfNumLogMsgs() : 0));

            // 21. 栈保护页。(fNumStackGuardPages) - 4字节, 默认 0 [cite: 68]
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfNumStackGuardPages() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_NumStackGuardPages",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfNumStackGuardPages() : 0));

            // 22. 初始化进程栈大小 (fInitProcStackSize) - 4字节, 默认 0x4000 [cite: 68]
            baos.write(longTo4BytesLittleEndian(settings != null ? settings.getfInitProcStackSize() : 0x4000L));
            fieldMap.put("Partition_" + partitionNameKey + "_InitProcStackSize",
                    longTo4BytesLittleEndian(settings != null ? settings.getfInitProcStackSize() : 0x4000L));

            // 23. 中断栈大小(fIsrStackSize) - 4字节, 默认 0x2000 [cite: 68]
            baos.write(longTo4BytesLittleEndian(settings != null ? settings.getfIsrStackSize() : 0x2000L));
            fieldMap.put("Partition_" + partitionNameKey + "_IsrStackSize",
                    longTo4BytesLittleEndian(settings != null ? settings.getfIsrStackSize() : 0x2000L));

            // 24. 用户堆空间大小(fUserHeapSpaceSize) - 4字节 (32位，unsigned long), 固定值0 [cite: 73]
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put("Partition_" + partitionNameKey + "_UserHeapSpaceSize", longTo4BytesLittleEndian(0L));

            // 25. select 服务队列大小 (fSelSvrQSize) - 4字节, 默认 0 [cite: 73]
            baos.write(intToBytesLittleEndian(settings != null ? (int) settings.getfSelSvrQSize() : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_SelSvrQSize",
                    intToBytesLittleEndian(settings != null ? (int) settings.getfSelSvrQSize() : 0));

            // 26. 动态空间分配禁止标。(fAllocDisableFlag) - 4字节, 默认 1 [cite: 73]
            baos.write(intToBytesLittleEndian(settings != null && !settings.isfAllocDisable() ? 0 : 1));
            fieldMap.put("Partition_" + partitionNameKey + "_AllocDisableFlag",
                    intToBytesLittleEndian(settings != null && !settings.isfAllocDisable() ? 0 : 1));

            // 在8字节字段前检查对齐
            add8ByteAlignmentIfNeeded(baos, fieldMap, "Partition_" + partitionNameKey + "_Padding_After26");

            // 27. 事件监控时序时间 (fMaxEventQStallDuration) - 8字节, 默认 -1 [cite: 73]
            baos.write(longTo8BytesLittleEndian(settings != null ? settings.getfMaxEventQStallDuration() : -1L));
            fieldMap.put("Partition_" + partitionNameKey + "_MaxEventQStallDuration",
                    longTo8BytesLittleEndian(settings != null ? settings.getfMaxEventQStallDuration() : -1L));

            // 28. 浮点异常使能标志 (fFpExcEnableFlag) - 4字节, 默认 1 [cite: 73]
            baos.write(intToBytesLittleEndian(settings != null && !settings.isfFpExcEnable() ? 0 : 1));
            fieldMap.put("Partition_" + partitionNameKey + "_FpExcEnableFlag",
                    intToBytesLittleEndian(settings != null && !settings.isfFpExcEnable() ? 0 : 1));

            // 29. 是否允许空闲时占用标。(fAppsIdleRelinquishEnabled) - 4字节, 默认 0 [cite: 73]
            baos.write(intToBytesLittleEndian(settings != null && settings.isfAppsIdleRelinquishEnabled() ? 1 : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_AppsIdleRelinquishEnabled",
                    intToBytesLittleEndian(settings != null && settings.isfAppsIdleRelinquishEnabled() ? 1 : 0));

            // 30. PPS 优先。(fAppsPriority) - 4字节, 默认 -1
            baos.write(intToBytesLittleEndian(settings != null ? settings.getfAppsPriority() : -1));
            fieldMap.put("Partition_" + partitionNameKey + "_AppsPriority",
                    intToBytesLittleEndian(settings != null ? settings.getfAppsPriority() : -1));

            // 31. 端口配置地址 (fPortConfigAddress) - 4字节 (32位，unsigned long), 预留, 固定 0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put("Partition_" + partitionNameKey + "_PortConfigAddress", longTo4BytesLittleEndian(0L));

            // 32. 分区端口数(fPartitionPortCount) - 4字节, 预留, 固定 0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put("Partition_" + partitionNameKey + "_PartitionPortCount", intToBytesLittleEndian(0));

            // 33. 系统分区标志 (fSystemPartitionFlag) - 4字节, 默认 0
            baos.write(intToBytesLittleEndian(settings != null && settings.getfSystemPartition() ? 1 : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_SystemPartitionFlag",
                    intToBytesLittleEndian(settings != null && settings.getfSystemPartition() ? 1 : 0));

            // 34. 分区允许切换调度表标。(fSetModuleScheduleFlag) - 4字节, 默认 0
            baos.write(intToBytesLittleEndian(settings != null && settings.isfSetModuleSchedule() ? 1 : 0));
            fieldMap.put("Partition_" + partitionNameKey + "_SetModuleScheduleFlag",
                    intToBytesLittleEndian(settings != null && settings.isfSetModuleSchedule() ? 1 : 0));

            // 35. 分区调度表切换动作(fScheduleChangeAction) - 4字节, 默认 3 (IGNORE)
            int action = 3;
            if (settings != null && settings.getfScheduleChangeAction() != null) {
                switch (settings.getfScheduleChangeAction().toUpperCase()) {
                    case "COLD_RESTART":
                        action = 0;
                        break;
                    case "WARM_RESTART":
                        action = 1;
                        break;
                    case "IDLE":
                        action = 2;
                        break;
                    case "IGNORE":
                        action = 3;
                        break;
                }
            }
            baos.write(intToBytesLittleEndian(action));
            fieldMap.put("Partition_" + partitionNameKey + "_ScheduleChangeAction", intToBytesLittleEndian(action));

            // 36. 分区内设备配置地址 - 4字节 (32位，unsigned long), 固定 0 [cite: 84]
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put("Partition_" + partitionNameKey + "_DeviceConfigAddress", longTo4BytesLittleEndian(0L));

            // 37. 分区内设备数 - 4字节, 固定 0 [cite: 84]
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put("Partition_" + partitionNameKey + "_DeviceCount", intToBytesLittleEndian(0));

            // 38. 分区内对象资源空间大小 - 4字节 (32位，unsigned long), 默认 0 [cite: 84]
            baos.write(longTo4BytesLittleEndian(settings != null ? settings.getfObjResSpaceSize() : 0L));
            fieldMap.put("Partition_" + partitionNameKey + "_ObjResSpaceSize",
                    longTo4BytesLittleEndian(settings != null ? settings.getfObjResSpaceSize() : 0L));

            // 39. 分区内任务异常栈大小 - 4字节 (32位，unsigned long), 固定 0x2000 [cite: 84]
            baos.write(longTo4BytesLittleEndian(0x2000L));
            fieldMap.put("Partition_" + partitionNameKey + "_TaskExceptionStackSize",
                    longTo4BytesLittleEndian(0x2000L));

            // 40. 分区内空闲任务栈大小 - 4字节 (32位，unsigned long), 固定 0x2000 [cite: 84]
            baos.write(longTo4BytesLittleEndian(0x2000L));
            fieldMap.put("Partition_" + partitionNameKey + "_IdleTaskStackSize", longTo4BytesLittleEndian(0x2000L));

            // 41. 分区内初始化任务栈大小 - 4字节 (32位，unsigned long), 固定 0x4000 [cite: 84]
            baos.write(longTo4BytesLittleEndian(0x4000L));
            fieldMap.put("Partition_" + partitionNameKey + "_InitTaskStackSize", longTo4BytesLittleEndian(0x4000L));

            // 42. 分区内初始化任务优先。- 4字节, 固定 10 [cite: 84]
            baos.write(intToBytesLittleEndian(10));
            fieldMap.put("Partition_" + partitionNameKey + "_InitTaskPriority", intToBytesLittleEndian(10));

            // 43. 分区缺省加载应用。- 32字节, 默认。[cite: 88]
            byte[] defaultAppNameBytes = fixedLengthAscii(settings != null ? settings.getfDefaultAppNameAsString() : "",
                    32);
            baos.write(defaultAppNameBytes);
            fieldMap.put("Partition_" + partitionNameKey + "_DefaultAppName", defaultAppNameBytes);

            // 44. 分区绑定的处理器核号 - 4字节 [cite: 88]
            long boundProcessorCore = partition.getfBoundProcessorCore();
            baos.write(longTo4BytesLittleEndian(boundProcessorCore));
            fieldMap.put("Partition_" + partitionNameKey + "_BoundProcessorCore",
                    longTo4BytesLittleEndian(boundProcessorCore));

            // 45. 亲和性设置权。- 4字节, 固定 0 [cite: 88]
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put("Partition_" + partitionNameKey + "_AffinitySettingPermission", intToBytesLittleEndian(0));

            // 46. GOS分区标记 - 4字节, 固定 0 [cite: 88]
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put("Partition_" + partitionNameKey + "_GOSPartitionFlag", longTo4BytesLittleEndian(0L));

            // 47. GOS分区配置地址 - 4字节 (32位，unsigned long), 固定 0 [cite: 88]
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put("Partition_" + partitionNameKey + "_GOSConfigAddress", longTo4BytesLittleEndian(0L));

            // 48. 用户扩展参数1 - 4字节, 默认 0 [cite: 88]
            byte[] user1Bytes = intToBytesLittleEndian(settings != null ? settings.getfUserExtension1() : 0);
            baos.write(user1Bytes);
            fieldMap.put("Partition_" + partitionNameKey + "_User1", user1Bytes);

            // 49. 用户扩展参数2 - 4字节, 默认 0 [cite: 88]
            byte[] user2Bytes = intToBytesLittleEndian(settings != null ? settings.getfUserExtension2() : 0);
            baos.write(user2Bytes);
            fieldMap.put("Partition_" + partitionNameKey + "_User2", user2Bytes);

            // 最后确保整个结构8字节对齐
            add8ByteAlignmentIfNeeded(baos, fieldMap, "Partition_" + partitionNameKey + "_AlignmentPadding");
        }
        return baos.toByteArray();
    }

    /**
     * add8ByteAlignmentIfNeeded
     *
     * @brief
     * 功能: 在8字节字段前检查并添加对齐如果当前位置不是8字节对齐，就添加padding。
     *
     * @param[in]  baos:        字节输出流
     * @param[in]  fieldMap:    字段映射表
     * @param[in]  paddingName: padding字段的名称
     * @throws IOException 写入异常
     * @scope internal
     * @errno:
     * N/A
     */
    private void add8ByteAlignmentIfNeeded(ByteArrayOutputStream baos,
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

    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 计算8字节对齐后的大小。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private int alignTo8Bytes(int size) {
        return (size + 7) & ~7;
    }

    /**
     * calculatePartitionLogBookConfigOffset
     *
     * @brief
     * 功能: 计算日志记事本配置存储结构在CPM配置信息可加载文件中的偏。?根据OsConfigGenerator32small中的偏移计算逻辑。?logConfigOffset = portConfigOffset + portConfigSize。
     *
     * @param[in]  module:    包含所有配置信息的Module对象
     * @param[in]  partition: 当前分区对象
     * @return 本分区所属的日志记事本配置存储结构的偏移量，如果没有则返。。
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculatePartitionLogBookConfigOffset(Module module, Partition partition, long baseOffset) {
        // 如果本分区没有关联的日志记事本，返回0
        if (getPartitionLogBookCount(module, partition) == 0) {
            return 0L;
        }

        // 计算本分区的第一个LogBook在所有LogBook中的索引
        // 需要遍历所有LogBook，累计在当前分区之前的LogBook数量
        int logBookIndex = 0;
        String currentPartitionName = partition.getfNameAsString();

        // 遍历所有分区，按照分区顺序累计LogBook数量
        for (Map.Entry<String, Partition> entry : module.getfPartitions().entrySet()) {
            Partition p = entry.getValue();
            String partitionName = p.getfNameAsString();

            // 如果是当前分区，停止计数
            if (partitionName.equals(currentPartitionName)) {
                break;
            }

            // 累计该分区的LogBook数量（从Module的fLogBooks中筛选）
            if (module.getfLogBooks() != null) {
                for (LogBook logBook : module.getfLogBooks().values()) {
                    if (logBook.getPartitionNameRef() != null &&
                            logBook.getPartitionNameRef().equals(partitionName)) {
                        logBookIndex++;
                    }
                }
            }
        }

        // 计算单个日志记事本配置的大小
        // fLogbookName(32) + fMaxMessageSize(4) + fMaxNBLoggedMessages(4) +
        // fMaxNBInProgressMessages(4) + fSerialNumber(4) = 48字节
        int singleLogBookConfigSize = 48;

        // 返回：基础偏移 + (当前分区第一个日志记事本的索引 * 单个配置大小)
        return baseOffset + ((long) logBookIndex * singleLogBookConfigSize);
    }

    /**
     * getPartitionLogBookCount
     *
     * @brief
     * 功能: 获取本分区关联的日志记事本数。。
     *
     * @param[in]  module:    模块对象
     * @param[in]  partition: 分区对象
     * @return 本分区关联的日志记事本数。。
     * @scope internal
     * @errno:
     * N/A
     */
    private int getPartitionLogBookCount(Module module, Partition partition) {
        // 从Module的fLogBooks中筛选出PartitionNameRef等于当前分区名的LogBook
        if (module.getfLogBooks() == null) {
            return 0;
        }

        String currentPartitionName = partition.getfNameAsString();
        int count = 0;

        for (LogBook logBook : module.getfLogBooks().values()) {
            if (logBook.getPartitionNameRef() != null &&
                    logBook.getPartitionNameRef().equals(currentPartitionName)) {
                count++;
            }
        }

        return count;
    }

    /**
     * calculatePartitionSharedDataRegionConfigOffset
     *
     * @brief
     * 功能: 计算本分区所属的第一个共享数据分区配置存储结构在CPM配置信息可加载文件中的偏移。
     *
     * @param[in]  module:     模块对象
     * @param[in]  partition:  当前分区对象
     * @param[in]  baseOffset: 共享数据区分区配置块在CPM配置信息可加载文件中的基础偏移（从OsConfigGenerator传入）
     * @return 本分区第一个共享数据区配置的偏移字节数，如果没有共享数据区则返回0
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculatePartitionSharedDataRegionConfigOffset(Module module, Partition partition, long baseOffset) {
        // 如果本分区没有关联的共享数据区，返回0
        if (partition.getfSharedDataRegions() == null || partition.getfSharedDataRegions().isEmpty()) {
            return 0L;
        }

        // 计算本分区的第一个SharedDataRegion在所有Partition的SharedDataRegion中的索引
        // 需要遍历所有Partition，累计在当前分区之前的SharedDataRegion数量
        int sharedDataRegionIndex = 0;
        String currentPartitionName = partition.getfNameAsString();
        boolean foundCurrentPartition = false;

        for (Map.Entry<String, Partition> entry : module.getfPartitions().entrySet()) {
            Partition p = entry.getValue();

            // 如果是当前分区，停止计数
            if (p.getfNameAsString().equals(currentPartitionName)) {
                foundCurrentPartition = true;
                break;
            }

            // 累计前面分区的SharedDataRegion数量
            if (p.getfSharedDataRegions() != null) {
                sharedDataRegionIndex += p.getfSharedDataRegions().size();
            }
        }

        // 如果没有找到当前分区，返回0
        if (!foundCurrentPartition) {
            return 0L;
        }

        // 计算单个共享数据区分区配置的大小（参考SharedDataRegionNameGenerator32small）
        // 共享数据区名(32) + MMU属性(4) + 8字节对齐填充(0-4) = 40字节（对齐后）
        int singleSharedDataRegionConfigSize = 40;

        // 返回：基础偏移 + (当前分区第一个共享数据区的索引 * 单个配置大小)
        return baseOffset + ((long) sharedDataRegionIndex * singleSharedDataRegionConfigSize);
    }

}
