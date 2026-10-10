/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreOSConfigGenerator64small.java
 * 
 * 描述    : 生成Core OS相关64位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Schedule;
import com.nwpu.verify.entity.ApplicationEntity;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.entity.QueuingPort;
import com.nwpu.verify.entity.SamplingPort;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
/**
 * @brief
 * 功能: Core OS配置二进制生成器（64位小端）。
 */
public class CoreOSConfigGenerator64small {

    // 配置类型固定值：0x43525403
    private static final int CONFIG_TYPE = 0x43525403;

    // 调度表数组大小
    private static final int SCHEDULE_TABLE_ARRAY_SIZE = 256;

    // 亲和组数组大
    private static final int AFFINITY_GROUP_ARRAY_SIZE = 255;

    // 分区配置记录数组大小
    private static final int PARTITION_CONFIG_ARRAY_SIZE = 255;

    /**
     * 生成核心OS配置存储结构的二进制数据
     * 
     * @param module                       模块对象，包含核心OS配置信息
     * @param fieldMap                     字段映射表，用于记录生成的二进制字段
     * @param cpmStartAddress              CPM配置信息可加载文件起始地址
     * @param healthMonitorConfigOffset    健康监控配置存储结构在文件中的偏。
     * @param scheduleConfigOffsets        调度表配置在文件中的偏移映射（ID -> 偏移。
     * @param partitionConfigOffsets       分区配置在文件中的偏移映射（分区。-> 偏移。
     * @param portConfigOffset             端口配置存储结构在文件中的偏。
     * @param sharedDataRegionConfigOffset 共享数据区配置存储结构在文件中的偏移
     * @return 核心OS配置存储结构的二进制数据
     * @throws IOException 写入异常
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress, long healthMonitorConfigOffset,
            Map<Integer, Long> scheduleConfigOffsets,
            Map<String, Long> partitionConfigOffsets,
            long portConfigOffset,
            long sharedDataRegionConfigOffset) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 配置类型 (4字节) - 固定。0x43525403"
        byte[] configTypeBytes = BinaryUtils.intToBytesLittleEndian(CONFIG_TYPE);
        baos.write(configTypeBytes);
        fieldMap.put("CoreOSConfig_ConfigType", configTypeBytes);

        // 2. 配置数据大小 (4字节)
        byte[] configDataSizeBytes = BinaryUtils.intToBytesLittleEndian(calculateStructSize());
        baos.write(configDataSizeBytes);
        fieldMap.put("CoreOSConfig_ConfigDataSize", configDataSizeBytes);

        // 3. 调度表数。(256个unsigned long)
        generateScheduleTableArray(module, fieldMap, baos, cpmStartAddress, scheduleConfigOffsets);

        // 4. 亲和组数。(255个unsigned long)
        generateAffinityGroupArray(module, fieldMap, baos);

        // 5. 健康监控配置存储结构地址 (8字节)
        long healthMonitorConfigAddress = cpmStartAddress + healthMonitorConfigOffset;
        byte[] healthMonitorConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(healthMonitorConfigAddress);
        baos.write(healthMonitorConfigAddressBytes);
        fieldMap.put("CoreOSConfig_HealthMonitorConfigAddress", healthMonitorConfigAddressBytes);

        // 6. 锁调度的时间 (8字节) - watchDogTimerDuration属性。
        generateWatchDogTimerDuration(module, fieldMap, baos);

        // 7. 分区配置记录数组 (255个unsigned long，每。字节，共1020字节)
        generatePartitionConfigArray(module, fieldMap, baos, cpmStartAddress, partitionConfigOffsets);

        // 8. 端口配置地址 (8字节)
        // CPM配置信息可加载文件起始地址+端口配置存储结构在CPM配置信息可加载文件中的偏移
        // 如果不存在端口配置，取0
        long portConfigAddress = 0L;
        if (hasAnyPorts(module)) {
            portConfigAddress = cpmStartAddress + portConfigOffset;
        }
        byte[] portConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(portConfigAddress);
        baos.write(portConfigAddressBytes);
        fieldMap.put("CoreOSConfig_PortConfigAddress", portConfigAddressBytes);

        // 9. 端口配置数(4字节)
        generatePortConfigCount(module, fieldMap, baos);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After9");

        // 10. 共享数据区配置地址 (8字节)
        // CPM配置信息可加载文件起始地址+共享数据区配存储结构在CPM配置信息可加载文件中的偏移
        // 如果不存在共享数据区，取0
        long sharedDataRegionConfigAddress = 0L;
        if (module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty()) {
            sharedDataRegionConfigAddress = cpmStartAddress + sharedDataRegionConfigOffset;
        }
        byte[] sharedDataRegionConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(sharedDataRegionConfigAddress);
        baos.write(sharedDataRegionConfigAddressBytes);
        fieldMap.put("CoreOSConfig_SharedDataRegionConfigAddress", sharedDataRegionConfigAddressBytes);

        // 11. 共享数据区配置数 (4字节)
        generateSharedDataRegionConfigCount(module, fieldMap, baos);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After11");

        // 12. 设备配置地址 (8字节) - 固定值0
        byte[] deviceConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(0L);
        baos.write(deviceConfigAddressBytes);
        fieldMap.put("CoreOSConfig_DeviceConfigAddress", deviceConfigAddressBytes);

        // 13. 分区设备配置数(4字节) - 固定值0
        byte[] partitionDeviceConfigCountBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(partitionDeviceConfigCountBytes);
        fieldMap.put("CoreOSConfig_PartitionDeviceConfigCount", partitionDeviceConfigCountBytes);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After13");

        // 14. 非安全系统调用超时时间(8字节)
        generateUnsafeMethodTimeout(module, fieldMap, baos);

        // 15. 模块ID (4字节)
        generateModuleId(module, fieldMap, baos);

        // 16. 动态空间分配禁止标志(4字节)
        generateMemoryAllocationDisabledFlag(module, fieldMap, baos);

        // 17. 系统时钟频率 (4字节)
        generateSysClkRateHz(module, fieldMap, baos);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After17");

        // 18. 逻辑空间基地址 (8字节)
        generateLogicalSpaceBaseAddress(module, fieldMap, baos);

        // 19. 逻辑空间大小 (8字节)
        generateLogicalSpaceSize(module, fieldMap, baos);

        // 20. 逻辑空间分配单位 (4字节)
        generateLogicalSpaceAllocationUnit(module, fieldMap, baos);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After20");

        // 21. 分区逻辑空间地址 (8字节)
        generatePartitionVirtualAddress(module, fieldMap, baos);

        // 22. 分区逻辑空间地址大小 (8字节)
        generatePartitionVirtualMemorySize(module, fieldMap, baos);

        // 23. 处理器核数(4字节)
        int processorCoreCount = 1;
        if (module.getfPartitions() != null && !module.getfPartitions().isEmpty()) {
            java.util.Set<Integer> boundCoreSet = new java.util.HashSet<>();
            for (Partition partition : module.getfPartitions().values()) {
                if (partition != null && partition.getfBoundProcessorCore() > 0) {
                    boundCoreSet.add(partition.getfBoundProcessorCore());
                }
            }
            if (!boundCoreSet.isEmpty()) {
                processorCoreCount = boundCoreSet.size();
            }
        }
        byte[] processorCoreCountBytes = BinaryUtils.intToBytesLittleEndian(processorCoreCount);
        baos.write(processorCoreCountBytes);
        fieldMap.put("CoreOSConfig_ProcessorCoreCount", processorCoreCountBytes);

        // 24. 分区多核运行模式 (4字节) - 固定值1
        byte[] partitionMulticoreModeBytes = BinaryUtils.intToBytesLittleEndian(1);
        baos.write(partitionMulticoreModeBytes);
        fieldMap.put("CoreOSConfig_PartitionMulticoreMode", partitionMulticoreModeBytes);

        // 25. 确定性分配模式(4字节) - 固定值0
        byte[] deterministicAllocationModeBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(deterministicAllocationModeBytes);
        fieldMap.put("CoreOSConfig_DeterministicAllocationMode", deterministicAllocationModeBytes);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After25");

        // 26. 中断服务配置地址 (8字节）
        byte[] interruptServiceConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(0L);
        baos.write(interruptServiceConfigAddressBytes);
        fieldMap.put("CoreOSConfig_InterruptServiceConfigAddress", interruptServiceConfigAddressBytes);

        // 27. 中断服务配置。(4字节) - 固定。
        byte[] interruptServiceConfigCountBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(interruptServiceConfigCountBytes);
        fieldMap.put("CoreOSConfig_InterruptServiceConfigCount", interruptServiceConfigCountBytes);

        // 28. 用户扩展参数1 (4字节)
        generateUserExtensionParam1(module, fieldMap, baos);

        // 29. 用户扩展参数2 (4字节)
        generateUserExtensionParam2(module, fieldMap, baos);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After29");

        // 30. 卷配置地址 (8字节)
        byte[] volumeConfigAddressBytes = BinaryUtils.longTo8BytesLittleEndian(0L);
        baos.write(volumeConfigAddressBytes);
        fieldMap.put("CoreOSConfig_VolumeConfigAddress", volumeConfigAddressBytes);

        // 31. 卷配置数 (4字节) - 固定值0
        byte[] volumeConfigCountBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(volumeConfigCountBytes);
        fieldMap.put("CoreOSConfig_VolumeConfigCount", volumeConfigCountBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_Final");

        byte[] result = baos.toByteArray();

        return result;
    }

    /**
     * 生成调度表数。
     * 数组大小固定。56。取值分别为对应调度表ID的调度表存储结构在CPM配置信息可加载文件中
     * 相对文件起始地址的偏。CPM配置信息可加载文件起始地址。
     * 即第0位置填写ID号等。的调度表配置数据结构的地址，第1位置填写ID号等。的调度表配置数据结构的地址。
     * 以此类推。如果相应的调度表不存在填正整数"0"。
     */
    private static void generateScheduleTableArray(Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos, long cpmStartAddress, Map<Integer, Long> scheduleConfigOffsets)
            throws IOException {

        for (int i = 0; i < SCHEDULE_TABLE_ARRAY_SIZE; i++) {
            long scheduleAddress = 0L; // 默认值为0

            // 检查是否存在ID为i的调度表
            if (scheduleConfigOffsets != null && scheduleConfigOffsets.containsKey(i)) {
                // 计算调度表在文件中的地址
                scheduleAddress = cpmStartAddress + scheduleConfigOffsets.get(i);
            } else if (module.getfSchedules() != null) {
                // 从模块中查找对应ID的调度表
                for (Schedule schedule : module.getfSchedules().values()) {
                    if (schedule.getScheduleIdentifier() == i) {
                        // 如果找到对应ID的调度表，但没有偏移信息，则设为0
                        // 实际使用时需要传入正确的scheduleConfigOffsets
                        scheduleAddress = 0L;
                        break;
                    }
                }
            }

            byte[] scheduleAddressBytes = BinaryUtils.longTo8BytesLittleEndian(scheduleAddress);
            baos.write(scheduleAddressBytes);
            fieldMap.put("CoreOSConfig_ScheduleTable_" + i, scheduleAddressBytes);
        }
    }

    /**
     * 生成亲和组数。
     * 数组大小固定。55。每个成员固定值正整数"0"
     */
    private static void generateAffinityGroupArray(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        for (int i = 0; i < AFFINITY_GROUP_ARRAY_SIZE; i++) {
            // 每个成员固定值正整数"0"
            byte[] affinityGroupBytes = BinaryUtils.longTo8BytesLittleEndian(0L);
            baos.write(affinityGroupBytes);
            fieldMap.put("CoreOSConfig_AffinityGroup_" + i, affinityGroupBytes);
        }
    }

    /**
     * 生成锁调度的时间
     * 。Module/CoreOS/CoreOSDescription/KernelConfiguration/@watchDogTimerDuration属性获取。
     */
    private static void generateWatchDogTimerDuration(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long watchDogTimerDuration = 0L; // 默认值

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            watchDogTimerDuration = module.getfCoreOS().getfKernelConfiguration().getfWatchDogTimerDuration();
        }

        // 转换。字节大端格式
        byte[] watchDogTimerDurationBytes = BinaryUtils.longTo8BytesLittleEndian(watchDogTimerDuration);
        baos.write(watchDogTimerDurationBytes);
        fieldMap.put("CoreOSConfig_WatchDogTimerDuration", watchDogTimerDurationBytes);
    }

    /**
     * 生成分区配置记录数组
     * 数组大小固定。55，分别赋值CPM配置信息可加载文件起始地址+分区配置存储结构在文件中的偏移。
     * 分区数不。55时，数组剩余成员赋值固定值正整数"0"。
     */
    private static void generatePartitionConfigArray(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos, long cpmStartAddress, Map<String, Long> partitionConfigOffsets)
            throws IOException {

        int partitionIndex = 0;

        // 遍历所有分区，按顺序填充数。
        if (module.getfPartitions() != null && partitionConfigOffsets != null) {
            for (String partitionName : module.getfPartitions().keySet()) {
                if (partitionIndex >= PARTITION_CONFIG_ARRAY_SIZE) {
                    break; // 超过255个分区，只取255个
                }

                long partitionAddress = 0L; // 默认值
                if (partitionConfigOffsets.containsKey(partitionName)) {
                    partitionAddress = cpmStartAddress + partitionConfigOffsets.get(partitionName);
                }

                byte[] partitionAddressBytes = BinaryUtils.longTo8BytesLittleEndian(partitionAddress);
                baos.write(partitionAddressBytes);
                fieldMap.put("CoreOSConfig_PartitionConfig_" + partitionIndex, partitionAddressBytes);
                partitionIndex++;
            }
        }

        // 填充剩余位置
        for (int i = partitionIndex; i < PARTITION_CONFIG_ARRAY_SIZE; i++) {
            byte[] zeroBytes = BinaryUtils.longTo8BytesLittleEndian(0L);
            baos.write(zeroBytes);
            fieldMap.put("CoreOSConfig_PartitionConfig_" + i, zeroBytes);
        }
    }

    /**
     * 生成端口配置。
     * 所有端口配置的个数。包括Application/ApplicationDescription/Ports和PseudoPartition/PseudoPartitionDescription/Ports下的所有端口，
     * 但不包括空端口，即Attribute属性值为"NULL_PORT"的端口
     */
    private static void generatePortConfigCount(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int portCount = 0;

        // 统计Application下的端口
        if (module.getfApplications() != null) {
            for (ApplicationEntity application : module.getfApplications().values()) {
                // 统计QueuingPort
                if (application.getfQueuingPorts() != null) {
                    for (QueuingPort port : application.getfQueuingPorts()) {
                        // 排除NULL_PORT
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            portCount++;
                        }
                    }
                }
                // 统计SamplingPort
                if (application.getfSamplingPorts() != null) {
                    for (SamplingPort port : application.getfSamplingPorts()) {
                        // 排除NULL_PORT
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            portCount++;
                        }
                    }
                }
            }
        }

        // 统计Partition下的端口（如果有PseudoPartition的话）
        if (module.getfPartitions() != null) {
            for (Partition partition : module.getfPartitions().values()) {
                // 统计QueuingPort
                if (partition.getfQueuingPorts() != null) {
                    for (QueuingPort port : partition.getfQueuingPorts()) {
                        // 排除NULL_PORT
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            portCount++;
                        }
                    }
                }
                // 统计SamplingPort
                if (partition.getfSamplingPorts() != null) {
                    for (SamplingPort port : partition.getfSamplingPorts()) {
                        // 排除NULL_PORT
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            portCount++;
                        }
                    }
                }
            }
        }

        byte[] portCountBytes = BinaryUtils.intToBytesLittleEndian(portCount);
        baos.write(portCountBytes);
        fieldMap.put("CoreOSConfig_PortConfigCount", portCountBytes);
    }

    /**
     * 生成共享数据区配置数
     * 共享数据区的个数，从/Module/SharedDataRegions中的SharedData元素个数获取
     */
    private static void generateSharedDataRegionConfigCount(Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int sharedDataRegionCount = 0;

        // 从Module的SharedDataRegions中统计SharedData的个数
        if (module.getfSharedDatas() != null) {
            sharedDataRegionCount = module.getfSharedDatas().size();
        }

        byte[] sharedDataRegionCountBytes = BinaryUtils.intToBytesLittleEndian(sharedDataRegionCount);
        baos.write(sharedDataRegionCountBytes);
        fieldMap.put("CoreOSConfig_SharedDataRegionConfigCount", sharedDataRegionCountBytes);
    }

    /**
     * 生成非安全系统调用超时时间
     * 。Module/CoreOS/CoreOSDescription/KernelConfiguration/@unsafeMethodTimeout属性获取。
     */
    private static void generateUnsafeMethodTimeout(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long unsafeMethodTimeout = 0L; // 默认值

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            unsafeMethodTimeout = module.getfCoreOS().getfKernelConfiguration().getfUnsafeMethodTimeout();
        }

        // 转换为8字节大端格式
        byte[] unsafeMethodTimeoutBytes = BinaryUtils.longTo8BytesLittleEndian(unsafeMethodTimeout);
        baos.write(unsafeMethodTimeoutBytes);
        fieldMap.put("CoreOSConfig_UnsafeMethodTimeout", unsafeMethodTimeoutBytes);
    }

    /**
     * 生成模块ID
     * 从Module/@Id属性获取值，默认值为0
     */
    private static void generateModuleId(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int moduleId = 0; // 默认值

        // 使用Module的Id字段
        moduleId = module.getfId();

        byte[] moduleIdBytes = BinaryUtils.intToBytesLittleEndian(moduleId);
        baos.write(moduleIdBytes);
        fieldMap.put("CoreOSConfig_ModuleId", moduleIdBytes);
    }

    /**
     * 生成动态空间分配禁止标志
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@memoryAllocationDisabled属性获取值
     * 属性取值为"true"时，填正整数"1"，属性取值为"false"时，填正整数"0"
     */
    private static void generateMemoryAllocationDisabledFlag(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int memoryAllocationDisabled = 0; // 默认值为0(false)

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            memoryAllocationDisabled = module.getfCoreOS().getfKernelConfiguration().getfAMemoryAllocationDisabled();
        }

        byte[] memoryAllocationDisabledBytes = BinaryUtils.intToBytesLittleEndian(memoryAllocationDisabled);
        baos.write(memoryAllocationDisabledBytes);
        fieldMap.put("CoreOSConfig_MemoryAllocationDisabled", memoryAllocationDisabledBytes);
    }

    /**
     * 生成系统时钟频率
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@sysClkRateHz属性获取值
     */
    private static void generateSysClkRateHz(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int sysClkRateHz = 0; // 默认值

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            sysClkRateHz = module.getfCoreOS().getfKernelConfiguration().getfSysClkRateHz();
        }

        byte[] sysClkRateHzBytes = BinaryUtils.intToBytesLittleEndian(sysClkRateHz);
        baos.write(sysClkRateHzBytes);
        fieldMap.put("CoreOSConfig_SysClkRateHz", sysClkRateHzBytes);
    }

    /**
     * 生成逻辑空间基地址
     * 从Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/@Base_Address属性获取值
     */
    private static void generateLogicalSpaceBaseAddress(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long logicalSpaceBaseAddress = 0L; // 默认值

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            // BaseAddress是long类型，直接获取
            logicalSpaceBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfBaseAddress();
        }

        byte[] logicalSpaceBaseAddressBytes = BinaryUtils.longTo8BytesLittleEndian(logicalSpaceBaseAddress);
        baos.write(logicalSpaceBaseAddressBytes);
        fieldMap.put("CoreOSConfig_LogicalSpaceBaseAddress", logicalSpaceBaseAddressBytes);
    }

    /**
     * 生成逻辑空间大小
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceSize属性获取值
     */
    private static void generateLogicalSpaceSize(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long logicalSpaceSize = 0L;

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            logicalSpaceSize = module.getfCoreOS().getfKernelConfiguration().getfAddressSpaceSize();
        }

        byte[] logicalSpaceSizeBytes = BinaryUtils.longTo8BytesLittleEndian(logicalSpaceSize);
        baos.write(logicalSpaceSizeBytes);
        fieldMap.put("CoreOSConfig_LogicalSpaceSize", logicalSpaceSizeBytes);
    }

    /**
     * 生成逻辑空间分配单位
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit属性获取值
     */
    private static void generateLogicalSpaceAllocationUnit(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int logicalSpaceAllocationUnit = 0; // 默认值0

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            logicalSpaceAllocationUnit = module.getfCoreOS().getfKernelConfiguration()
                    .getfAddressSpaceRegionAllocationUnit();
        }

        byte[] logicalSpaceAllocationUnitBytes = BinaryUtils.intToBytesLittleEndian(logicalSpaceAllocationUnit);
        baos.write(logicalSpaceAllocationUnitBytes);
        fieldMap.put("CoreOSConfig_LogicalSpaceAllocationUnit", logicalSpaceAllocationUnitBytes);
    }

    /**
     * 生成分区逻辑空间地址
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@partitionVirtualAddress属性获取值
     */
    private static void generatePartitionVirtualAddress(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long partitionVirtualAddress = 0L;

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            partitionVirtualAddress = module.getfCoreOS().getfKernelConfiguration().getfPartitionVirtualAddress();
        }

        byte[] partitionVirtualAddressBytes = BinaryUtils.longTo8BytesLittleEndian(partitionVirtualAddress);
        baos.write(partitionVirtualAddressBytes);
        fieldMap.put("CoreOSConfig_PartitionVirtualAddress", partitionVirtualAddressBytes);
    }

    /**
     * 生成分区逻辑空间地址大小
     * 取值为partitionVirtualMemorySize的属性值，默认值为/Module/Partitions/Partition/PartitionDescription/Settings/@RequiredMemorySize中的最大值
     */
    private static void generatePartitionVirtualMemorySize(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long partitionVirtualMemorySize = 0L; // 默认值0

        // 首先尝试从KernelConfiguration获取partitionVirtualMemorySize
        // 注意：这个字段可能不存在于当前的实体类中，需要确定
        // if (module.getfCoreOS() != null &&
        // module.getfCoreOS().getfKernelConfiguration() != null) {
        // partitionVirtualMemorySize =
        // module.getfCoreOS().getfKernelConfiguration().getPartitionVirtualMemorySize();
        // }

        // 如果没有配置，则计算所有分区中RequiredMemorySize的最大值
        if (partitionVirtualMemorySize == 0L && module.getfPartitions() != null) {
            long maxRequiredMemorySize = 0L;
            for (Partition partition : module.getfPartitions().values()) {
                if (partition.getfSettings() != null) {
                    long requiredMemorySize = partition.getfSettings().getfRequiredMemorySize();
                    if (requiredMemorySize > maxRequiredMemorySize) {
                        maxRequiredMemorySize = requiredMemorySize;
                    }
                }
            }
            partitionVirtualMemorySize = maxRequiredMemorySize;
        }

        byte[] partitionVirtualMemorySizeBytes = BinaryUtils.longTo8BytesLittleEndian(partitionVirtualMemorySize);
        baos.write(partitionVirtualMemorySizeBytes);
        fieldMap.put("CoreOSConfig_PartitionVirtualMemorySize", partitionVirtualMemorySizeBytes);
    }

    /**
     * 生成用户扩展参数1
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@usr1属性获取值，默认值为0
     */
    private static void generateUserExtensionParam1(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int userExtensionParam1 = 0; // 默认值为0

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            int configuredValue = module.getfCoreOS().getfKernelConfiguration().getfUser1();
            // 如果配置文件中设置了非0值，则使用配置值；否则使用默认值0
            if (configuredValue != 0) {
                userExtensionParam1 = configuredValue;
            }
        }

        byte[] userExtensionParam1Bytes = BinaryUtils.intToBytesLittleEndian(userExtensionParam1);
        baos.write(userExtensionParam1Bytes);
        fieldMap.put("CoreOSConfig_UserExtensionParam1", userExtensionParam1Bytes);
    }

    /**
     * 生成用户扩展参数2
     * 从Module/CoreOS/CoreOSDescription/KernelConfiguration/@usr2属性获取值，默认值为0
     */
    private static void generateUserExtensionParam2(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        int userExtensionParam2 = 0; // 默认值为0

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            userExtensionParam2 = module.getfCoreOS().getfKernelConfiguration().getfUser2();
        }

        byte[] userExtensionParam2Bytes = BinaryUtils.intToBytesLittleEndian(userExtensionParam2);
        baos.write(userExtensionParam2Bytes);
        fieldMap.put("CoreOSConfig_UserExtensionParam2", userExtensionParam2Bytes);
    }
    /**
     * calculateStructSize
     *
     * @brief
     * 功能: 执行calculateStructSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static int calculateStructSize() {
        int size = 0;

        // 1-2: 配置类型 + 配置数据大小
        size += 4 + 4;

        // 3: 调度表数组
        size += SCHEDULE_TABLE_ARRAY_SIZE * 8;

        // 4: 亲和组数组
        size += AFFINITY_GROUP_ARRAY_SIZE * 8;

        // 5-6: 健康监控配置地址 + 锁调度时间
        size += 8 + 8;

        // 7: 分区配置记录数组
        size += PARTITION_CONFIG_ARRAY_SIZE * 8;

        // 8-9: 端口配置地址 + 端口配置数
        size += 8 + 4;
        // 检查对齐 (字段9后)
        size = addPaddingIfNeeded(size);

        // 10-11: 共享数据区配置地址 + 共享数据区配置数
        size += 8 + 4;
        // 检查对齐 (字段11后)
        size = addPaddingIfNeeded(size);

        // 12-13: 设备配置地址 + 分区设备配置数
        size += 8 + 4;
        // 检查对齐 (字段13后)
        size = addPaddingIfNeeded(size);

        // 14-17: 非安全系统调用超时时间 + 模块ID + 动态空间分配禁止标志 + 系统时钟频率
        size += 8 + 4 + 4 + 4;
        // 检查对齐 (字段17后)
        size = addPaddingIfNeeded(size);

        // 18-20: 逻辑空间基地址 + 逻辑空间大小 + 逻辑空间分配单位
        size += 8 + 8 + 4;
        // 检查对齐 (字段20后)
        size = addPaddingIfNeeded(size);

        // 21-25: 分区逻辑空间地址 + 分区逻辑空间大小 + 处理器核数 + 分区多核运行模式 + 确定性分配模式
        size += 8 + 8 + 4 + 4 + 4;
        // 检查对齐 (字段25后)
        size = addPaddingIfNeeded(size);

        // 26-27: 中断服务配置地址 + 中断服务配置数
        size += 8 + 4;

        // 28-29: 用户扩展参数1 + 用户扩展参数2
        size += 4 + 4;
        // 检查对齐 (字段29后)
        size = addPaddingIfNeeded(size);

        // 30-31: 卷配置地址 + 卷配置数
        size += 8 + 4;

        // 最终对齐
        size = alignTo8Bytes(size);

        return size;
    }

    /**
     * 在8字节字段前检查是否需要padding来达到8字节对齐
     * 如果需要，添加4字节padding
     */
    private static void add8ByteAlignmentIfNeeded(ByteArrayOutputStream baos,
            Map<String, byte[]> fieldMap, String paddingName) throws IOException {
        int currentSize = baos.size();
        if (currentSize % 8 != 0) {
            int paddingSize = 8 - (currentSize % 8);
            byte[] paddingBytes = new byte[paddingSize];
            baos.write(paddingBytes);
            fieldMap.put(paddingName, paddingBytes);
        }
    }
    /**
     * addPaddingIfNeeded
     *
     * @brief
     * 功能: 执行addPaddingIfNeeded处理逻辑。
     *
     * @param[in]  currentSize: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int addPaddingIfNeeded(int currentSize) {
        if (currentSize % 8 != 0) {
            return alignTo8Bytes(currentSize);
        }
        return currentSize;
    }
    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 执行alignTo8Bytes处理逻辑。
     *
     * @param[in]  size: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int alignTo8Bytes(int size) {
        return ((size + 7) / 8) * 8;
    }
    /**
     * hasAnyPorts
     *
     * @brief
     * 功能: 执行hasAnyPorts处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean hasAnyPorts(Module module) {
        // 检查Application下的端口
        if (module.getfApplications() != null) {
            for (ApplicationEntity application : module.getfApplications().values()) {
                // 检查QueuingPort
                if (application.getfQueuingPorts() != null) {
                    for (QueuingPort port : application.getfQueuingPorts()) {
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            return true;
                        }
                    }
                }
                // 检查SamplingPort
                if (application.getfSamplingPorts() != null) {
                    for (SamplingPort port : application.getfSamplingPorts()) {
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            return true;
                        }
                    }
                }
            }
        }

        // 检查Partition下的端口
        if (module.getfPartitions() != null) {
            for (Partition partition : module.getfPartitions().values()) {
                // 检查QueuingPort
                if (partition.getfQueuingPorts() != null) {
                    for (QueuingPort port : partition.getfQueuingPorts()) {
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            return true;
                        }
                    }
                }
                // 检查SamplingPort
                if (partition.getfSamplingPorts() != null) {
                    for (SamplingPort port : partition.getfSamplingPorts()) {
                        if (port.getfAttribute() != null && !"NULL_PORT".equals(port.getfAttribute())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
