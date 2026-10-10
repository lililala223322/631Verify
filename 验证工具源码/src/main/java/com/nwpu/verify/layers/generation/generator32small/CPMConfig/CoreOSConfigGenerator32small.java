/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreOSConfigGenerator32small.java
 * 
 * 描述    : 生成CoreOSConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

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
 * 功能: 核心OS配置存储结构生成器（32位大端序）根据32位规范生成核心OS配置数据存储结构。
 */
public class CoreOSConfigGenerator32small {

    // 配置类型固定值：0x43525403
    private static final int CONFIG_TYPE = 0x43525403;

    // 调度表数组大小
    private static final int SCHEDULE_TABLE_ARRAY_SIZE = 256;

    // 亲和组数组大
    private static final int AFFINITY_GROUP_ARRAY_SIZE = 255;

    // 分区配置记录数组大小
    private static final int PARTITION_CONFIG_ARRAY_SIZE = 255;

    /**
     * generate
     *
     * @brief
     * 功能: 生成核心OS配置存储结构的二进制数据。
     *
     * @param[in]  module:                       模块对象，包含核心OS配置信息
     * @param[in]  fieldMap:                     字段映射表，用于记录生成的二进制字段
     * @param[in]  cpmStartAddress:              CPM配置信息可加载文件起始地址
     * @param[in]  healthMonitorConfigOffset:    健康监控配置存储结构在文件中的偏。。
     * @param[in]  scheduleConfigOffsets:        调度表配置在文件中的偏移映射（ID -> 偏移。。
     * @param[in]  partitionConfigOffsets:       分区配置在文件中的偏移映射（分区。?-> 偏移。。
     * @param[in]  portConfigOffset:             端口配置存储结构在文件中的偏。。
     * @param[in]  sharedDataRegionConfigOffset: 共享数据区配置存储结构在文件中的偏移
     * @return 核心OS配置存储结构的二进制数据
     * @throws IOException 写入异常
     * @scope external
     * @errno:
     * N/A
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

        // 5. 健康监控配置存储结构地址 (4字节，32位系统)
        long healthMonitorConfigAddress = cpmStartAddress + healthMonitorConfigOffset;
        byte[] healthMonitorConfigAddressBytes = BinaryUtils.intToBytesLittleEndian((int) healthMonitorConfigAddress);
        baos.write(healthMonitorConfigAddressBytes);
        fieldMap.put("CoreOSConfig_HealthMonitorConfigAddress", healthMonitorConfigAddressBytes);

        // 在8字节signed long long字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After5");

        // 6. 锁调度的时间 (8字节，signed long long) - watchDogTimerDuration属性值
        generateWatchDogTimerDuration(module, fieldMap, baos);

        // 7. 分区配置记录数组 (255个unsigned long，每。字节，共1020字节)
        generatePartitionConfigArray(module, fieldMap, baos, cpmStartAddress, partitionConfigOffsets);

        // 8. 端口配置地址 (4字节，32位系统)
        // CPM配置信息可加载文件起始地址+端口配置存储结构在CPM配置信息可加载文件中的偏移
        // 如果不存在端口配置，取0
        long portConfigAddress = 0L;
        if (hasAnyPorts(module)) {
            portConfigAddress = cpmStartAddress + portConfigOffset;
        }
        byte[] portConfigAddressBytes = BinaryUtils.intToBytesLittleEndian((int) portConfigAddress);
        baos.write(portConfigAddressBytes);
        fieldMap.put("CoreOSConfig_PortConfigAddress", portConfigAddressBytes);

        // 9. 端口配置数(4字节)
        generatePortConfigCount(module, fieldMap, baos);

        // 10. 共享数据区配置地址 (4字节，32位系统)
        // CPM配置信息可加载文件起始地址+共享数据区配存储结构在CPM配置信息可加载文件中的偏移
        // 如果不存在共享数据区，取0
        long sharedDataRegionConfigAddress = 0L;
        if (module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty()) {
            sharedDataRegionConfigAddress = cpmStartAddress + sharedDataRegionConfigOffset;
        }
        byte[] sharedDataRegionConfigAddressBytes = BinaryUtils
                .intToBytesLittleEndian((int) sharedDataRegionConfigAddress);
        baos.write(sharedDataRegionConfigAddressBytes);
        fieldMap.put("CoreOSConfig_SharedDataRegionConfigAddress", sharedDataRegionConfigAddressBytes);

        // 11. 共享数据区配置数 (4字节)
        generateSharedDataRegionConfigCount(module, fieldMap, baos);

        // 12. 设备配置地址 (4字节，32位系统) - 固定值0
        byte[] deviceConfigAddressBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(deviceConfigAddressBytes);
        fieldMap.put("CoreOSConfig_DeviceConfigAddress", deviceConfigAddressBytes);

        // 13. 分区设备配置数(4字节) - 固定值0
        byte[] partitionDeviceConfigCountBytes = BinaryUtils.intToBytesLittleEndian(0);
        baos.write(partitionDeviceConfigCountBytes);
        fieldMap.put("CoreOSConfig_PartitionDeviceConfigCount", partitionDeviceConfigCountBytes);

        // 在8字节signed long long字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSConfig_Padding_After13");

        // 14. 非安全系统调用超时时间(8字节，signed long long)
        generateUnsafeMethodTimeout(module, fieldMap, baos);

        // 15. 模块ID (4字节)
        generateModuleId(module, fieldMap, baos);

        // 16. 动态空间分配禁止标志(4字节)
        generateMemoryAllocationDisabledFlag(module, fieldMap, baos);

        // 17. 系统时钟频率 (4字节)
        generateSysClkRateHz(module, fieldMap, baos);

        // 18. 逻辑空间基地址 (4字节，32位系统)
        generateLogicalSpaceBaseAddress(module, fieldMap, baos);

        // 19. 逻辑空间大小 (4字节，32位系统 unsigned int)
        generateLogicalSpaceSize(module, fieldMap, baos);

        // 20. 逻辑空间分配单位 (4字节)
        generateLogicalSpaceAllocationUnit(module, fieldMap, baos);

        // 21. 分区逻辑空间地址 (4字节，32位系统)
        generatePartitionVirtualAddress(module, fieldMap, baos);

        // 22. 分区逻辑空间地址大小 (4字节，32位系统 unsigned long)
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

        // 26. 中断服务配置地址 (4字节，32位系统)
        byte[] interruptServiceConfigAddressBytes = BinaryUtils.intToBytesLittleEndian(0);
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

        // 30. 卷配置地址 (4字节，32位系统)
        byte[] volumeConfigAddressBytes = BinaryUtils.intToBytesLittleEndian(0);
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
     * generateScheduleTableArray
     *
     * @brief
     * 功能: 生成调度表数。?数组大小固定。?56。取值分别为对应调度表ID的调度表存储结构在CPM配置信息可加载文件中相对文件起始地址的偏。?CPM配置信息可加载文件起始地址。?即第0位置填写ID号等。?的调度表配置数据结构的地址，第1位置填写ID号等。?的调度表配置数据结构的地址。?以此类推。如果相应的调度表不存在填正整数"0"。。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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

            byte[] scheduleAddressBytes = BinaryUtils.intToBytesLittleEndian((int) scheduleAddress);
            baos.write(scheduleAddressBytes);
            fieldMap.put("CoreOSConfig_ScheduleTable_" + i, scheduleAddressBytes);
        }
    }

    /**
     * generateAffinityGroupArray
     *
     * @brief
     * 功能: 生成亲和组数。?数组大小固定。?55。每个成员固定值正整数"0"。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateAffinityGroupArray(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        for (int i = 0; i < AFFINITY_GROUP_ARRAY_SIZE; i++) {
            // 每个成员固定值正整数"0" (4字节，32位系统)
            byte[] affinityGroupBytes = BinaryUtils.intToBytesLittleEndian(0);
            baos.write(affinityGroupBytes);
            fieldMap.put("CoreOSConfig_AffinityGroup_" + i, affinityGroupBytes);
        }
    }

    /**
     * generateWatchDogTimerDuration
     *
     * @brief
     * 功能: 生成锁调度的时间。?Module/CoreOS/CoreOSDescription/KernelConfiguration/@watchDogTimerDuration属性获取。。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generatePartitionConfigArray
     *
     * @brief
     * 功能: 生成分区配置记录数组数组大小固定。?55，分别赋值CPM配置信息可加载文件起始地址+分区配置存储结构在文件中的偏移。?分区数不。?55时，数组剩余成员赋值固定值正整数"0"。。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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

                byte[] partitionAddressBytes = BinaryUtils.intToBytesLittleEndian((int) partitionAddress);
                baos.write(partitionAddressBytes);
                fieldMap.put("CoreOSConfig_PartitionConfig_" + partitionIndex, partitionAddressBytes);
                partitionIndex++;
            }
        }

        // 填充剩余位置 (4字节，32位系统)
        for (int i = partitionIndex; i < PARTITION_CONFIG_ARRAY_SIZE; i++) {
            byte[] zeroBytes = BinaryUtils.intToBytesLittleEndian(0);
            baos.write(zeroBytes);
            fieldMap.put("CoreOSConfig_PartitionConfig_" + i, zeroBytes);
        }
    }

    /**
     * generatePortConfigCount
     *
     * @brief
     * 功能: 生成端口配置。?所有端口配置的个数。包括Application/ApplicationDescription/Ports和PseudoPartition/PseudoPartitionDescription/Ports下的所有端口，但不包括空端口，即Attribute属性值为"NULL_PORT"的端口。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateSharedDataRegionConfigCount
     *
     * @brief
     * 功能: 生成共享数据区配置数共享数据区的个数，从/Module/SharedDataRegions中的SharedData元素个数获取。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateUnsafeMethodTimeout
     *
     * @brief
     * 功能: 生成非安全系统调用超时时间。?Module/CoreOS/CoreOSDescription/KernelConfiguration/@unsafeMethodTimeout属性获取。。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateModuleId
     *
     * @brief
     * 功能: 生成模块ID从Module/@Id属性获取值，默认值为0。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateMemoryAllocationDisabledFlag
     *
     * @brief
     * 功能: 生成动态空间分配禁止标志从Module/CoreOS/CoreOSDescription/KernelConfiguration/@memoryAllocationDisabled属性获取值属性取值为"true"时，填正整数"1"，属性取值为"false"时，填正整数"0"。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateSysClkRateHz
     *
     * @brief
     * 功能: 生成系统时钟频率从Module/CoreOS/CoreOSDescription/KernelConfiguration/@sysClkRateHz属性获取值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateLogicalSpaceBaseAddress
     *
     * @brief
     * 功能: 生成逻辑空间基地址从Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/@Base_Address属性获取值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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

        byte[] logicalSpaceBaseAddressBytes = BinaryUtils.intToBytesLittleEndian((int) logicalSpaceBaseAddress);
        baos.write(logicalSpaceBaseAddressBytes);
        fieldMap.put("CoreOSConfig_LogicalSpaceBaseAddress", logicalSpaceBaseAddressBytes);
    }

    /**
     * generateLogicalSpaceSize
     *
     * @brief
     * 功能: 生成逻辑空间大小从Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceSize属性获取值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateLogicalSpaceSize(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long logicalSpaceSize = 0L;

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            logicalSpaceSize = module.getfCoreOS().getfKernelConfiguration().getfAddressSpaceSize();
        }

        byte[] logicalSpaceSizeBytes = BinaryUtils.intToBytesLittleEndian((int) logicalSpaceSize);
        baos.write(logicalSpaceSizeBytes);
        fieldMap.put("CoreOSConfig_LogicalSpaceSize", logicalSpaceSizeBytes);
    }

    /**
     * generateLogicalSpaceAllocationUnit
     *
     * @brief
     * 功能: 生成逻辑空间分配单位从Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit属性获取值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generatePartitionVirtualAddress
     *
     * @brief
     * 功能: 生成分区逻辑空间地址从Module/CoreOS/CoreOSDescription/KernelConfiguration/@partitionVirtualAddress属性获取值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generatePartitionVirtualAddress(com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap,
            ByteArrayOutputStream baos) throws IOException {

        long partitionVirtualAddress = 0L;

        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {
            partitionVirtualAddress = module.getfCoreOS().getfKernelConfiguration().getfPartitionVirtualAddress();
        }

        byte[] partitionVirtualAddressBytes = BinaryUtils.intToBytesLittleEndian((int) partitionVirtualAddress);
        baos.write(partitionVirtualAddressBytes);
        fieldMap.put("CoreOSConfig_PartitionVirtualAddress", partitionVirtualAddressBytes);
    }

    /**
     * generatePartitionVirtualMemorySize
     *
     * @brief
     * 功能: 生成分区逻辑空间地址大小取值为partitionVirtualMemorySize的属性值，默认值为/Module/Partitions/Partition/PartitionDescription/Settings/@RequiredMemorySize中的最大值。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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

        byte[] partitionVirtualMemorySizeBytes = BinaryUtils.intToBytesLittleEndian((int) partitionVirtualMemorySize);
        baos.write(partitionVirtualMemorySizeBytes);
        fieldMap.put("CoreOSConfig_PartitionVirtualMemorySize", partitionVirtualMemorySizeBytes);
    }

    /**
     * generateUserExtensionParam1
     *
     * @brief
     * 功能: 生成用户扩展参数1从Module/CoreOS/CoreOSDescription/KernelConfiguration/@usr1属性获取值，默认值为0。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * generateUserExtensionParam2
     *
     * @brief
     * 功能: 生成用户扩展参数2从Module/CoreOS/CoreOSDescription/KernelConfiguration/@usr2属性获取值，默认值为0。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * 功能: 计算核心OS配置存储结构的总大小（包含动态8字节对齐padding）32位系统版本 - unsigned long字段使用4字节。
     * @return N/A
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateStructSize() {
        int size = 0;

        // 1-2: 配置类型 + 配置数据大小
        size += 4 + 4;

        // 3: 调度表数组 (32位: 4字节*256)
        size += SCHEDULE_TABLE_ARRAY_SIZE * 4;

        // 4: 亲和组数组 (32位: 4字节*255)
        size += AFFINITY_GROUP_ARRAY_SIZE * 4;

        // 5: 健康监控配置地址 (32位: 4字节)
        size += 4;
        // 检查对齐 (在8字节signed long long字段前)
        size = addPaddingIfNeeded(size);

        // 6: 锁调度时间 (signed long long: 8字节)
        size += 8;

        // 7: 分区配置记录数组 (32位: 4字节*255)
        size += PARTITION_CONFIG_ARRAY_SIZE * 4;

        // 8-9: 端口配置地址(4字节) + 端口配置数(4字节)
        size += 4 + 4;

        // 10-11: 共享数据区配置地址(4字节) + 共享数据区配置数(4字节)
        size += 4 + 4;

        // 12-13: 设备配置地址(4字节) + 分区设备配置数(4字节)
        size += 4 + 4;
        // 检查对齐 (在8字节signed long long字段前)
        size = addPaddingIfNeeded(size);

        // 14: 非安全系统调用超时时间 (signed long long: 8字节)
        size += 8;

        // 15-17: 模块ID(4字节) + 动态空间分配禁止标志(4字节) + 系统时钟频率(4字节)
        size += 4 + 4 + 4;

        // 18-20: 逻辑空间基地址(4字节) + 逻辑空间大小(4字节) + 逻辑空间分配单位(4字节)
        size += 4 + 4 + 4;

        // 21-25: 分区逻辑空间地址(4字节) + 分区逻辑空间大小(4字节) + 处理器核数(4字节) + 分区多核运行模式(4字节) +
        // 确定性分配模式(4字节)
        size += 4 + 4 + 4 + 4 + 4;

        // 26-27: 中断服务配置地址(4字节) + 中断服务配置数(4字节)
        size += 4 + 4;

        // 28-29: 用户扩展参数1(4字节) + 用户扩展参数2(4字节)
        size += 4 + 4;

        // 30-31: 卷配置地址(4字节) + 卷配置数(4字节)
        size += 4 + 4;

        // 最终对齐
        size = alignTo8Bytes(size);

        return size;
    }

    /**
     * add8ByteAlignmentIfNeeded
     *
     * @brief
     * 功能: 在8字节字段前检查是否需要padding来达到8字节对齐如果需要，添加4字节padding。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
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
     * 功能: 辅助方法：检查当前大小是否需要padding来达到8字节对齐如果需要，返回对齐后的大小；否则返回原大小。
     * @return N/A
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
     * 功能: 将给定的大小向上取整到8字节对齐。
     * @return N/A
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
     * 功能: 检查模块中是否存在任何端口配置。
     *
     * @param[in]  module: 模块对象
     * @return 如果存在端口返回true，否则返回false
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
