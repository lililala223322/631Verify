/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionDeploymentGenerator32big.java
 * 
 * 描述    : 生成PartitionDeployment相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-25  开发人员  屏蔽未被需求规定的应用程序查找警告，保留缺失应用时跳过处理。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMDeploy;

import com.nwpu.verify.entity.ApplicationEntity;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.entity.Payload;
import com.nwpu.verify.entity.SharedLibrary;
import com.nwpu.verify.entity.CoreOS;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;
import com.nwpu.verify.layers.output.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @brief
 * 功能: 分区部署信息配置存储结构生成器 (32位大端)。
 */
public class PartitionDeploymentGenerator32big {

    // 固定值常量
    private static final int DOMAIN_TYPE = 3; // 非Online分区域类型
    private static final int ONLINE_DOMAIN_TYPE = 4; // Online分区域类型
    private static final int PLACEHOLDER = 0; // 占位符固定值
    private static final int CHECKSUM = 0x1234; // 校验和固定值

    // 段类型常量
    private static final int SEGMENT_TYPE_TEXT = 0x1; // .text段
    private static final int SEGMENT_TYPE_RODATA = 0x8; // .rodata段
    private static final int SEGMENT_TYPE_DATA = 0x2; // .data段
    private static final int SEGMENT_TYPE_PDATA = 0x10; // .pdata段
    private static final int SEGMENT_TYPE_BSS = 0x4; // .bss段
    private static final int SEGMENT_TYPE_PBSS = 0x20; // .pbss段

    /**
     * 根据PartitionPayload/@Online计算域类型。
     * 规范4.2.3规定：Online=true时为4，否则为3。
     */
    private int getDomainType(com.nwpu.verify.entity.Module module, String partitionName) {
        if (module != null && module.getfPayload() != null
                && module.getfPayload().getfPartitionPayloads() != null) {
            for (Payload.PartitionPayload payload : module.getfPayload().getfPartitionPayloads()) {
                if (partitionName != null && partitionName.equals(payload.getfNameRefAsString())) {
                    return payload.isOnline() ? ONLINE_DOMAIN_TYPE : DOMAIN_TYPE;
                }
            }
        }
        return DOMAIN_TYPE;
    }

    /**
     * generateAllPartitionDeployments
     *
     * @brief
     * 功能: 主控生成方法：为所有分区生成部署信息。
     * @return N/A
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateAllPartitionDeployments(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 分区物理地址跟踪
        PartitionPhysicalAddressTracker physicalTracker = new PartitionPhysicalAddressTracker(module);

        // 遍历所有分区
        int traversalIndex = 0; // 遍历顺序索引，用于物理地址计算
        for (Partition partition : module.getfPartitions().values()) {
            ApplicationEntity app = module.getfApplications().get(partition.getfApplicationNameRef());
            if (app == null) {
                continue;
            }

            // 获取当前分区在Payloads中的索引（用于部署地址计算）
            int partitionIndexInPayloads = getPartitionIndexInPayloads(module, partition.getfNameAsString());
            if (partitionIndexInPayloads < 0) {
                System.err.println("Warning: partition '" + partition.getfNameAsString() + "' not found in Payloads");
                continue;
            }

            // 创建段状态跟踪器
            SegmentStateTracker state = new SegmentStateTracker();
            initializeSegmentState(partition, app, module, state, partitionIndexInPayloads, traversalIndex,
                    physicalTracker);

            // 按顺序生成各个段
            baos.write(generateTextSegment(partition, app, module, fieldMap, state));
            baos.write(generateRodataSegment(partition, app, module, fieldMap, state));
            baos.write(generateDataSegment(partition, app, module, fieldMap, state));

            // 只有当XML中配置了.pdata段时才生成（通过标志位判断）
            if (app.getfMemorySize().hasPersistentData()) {
                baos.write(generatePdataSegment(partition, app, module, fieldMap, state));
            }

            baos.write(generateBssSegment(partition, app, module, fieldMap, state));

            // 只有当XML中配置了.pbss段时才生成（通过标志位判断）
            if (app.getfMemorySize().hasPersistentBss()) {
                baos.write(generatePbssSegment(partition, app, module, fieldMap, state));
            }

            // 更新物理地址跟踪器（为下一个分区做准备）
            physicalTracker.updateLastPartition(partition);
            traversalIndex++;
        }

        return baos.toByteArray();
    }

    /**
     * initializeSegmentState
     *
     * @brief
     * 功能: 初始化段状态跟踪器。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private void initializeSegmentState(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            SegmentStateTracker state, int partitionIndexInPayloads, int traversalIndex,
            PartitionPhysicalAddressTracker physicalTracker) {
        // 获取分区的ROM部署地址（Payload基地址）- 使用Payloads中的索引
        state.textRomAddress = getPartitionRomBaseAddress(partition, module, partitionIndexInPayloads);

        // 获取分区的虚拟地址（逻辑地址）
        state.textLogicalAddress = module.getfCoreOS().getfKernelConfiguration().getfPartitionVirtualAddress();

        // 计算分区的物理地址 - 使用遍历索引
        state.textPhysicalAddress = physicalTracker.getPhysicalAddressForPartition(partition, traversalIndex);
    }

    /**
     * generateTextSegment
     *
     * @brief
     * 功能: 4.2.3.1 分区部署信息.text段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateTextSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Text_";

        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值3
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值0
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值0x1
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_TEXT);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - text段ROM部署地址
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(state.textRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - text段虚拟地址
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(state.textLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节，unsigned long long) - text段物理地址
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(state.textPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeText属性值
        long textSize = app.getfMemorySize().getfMemorySizeText();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(textSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "SegmentSize", segmentSizeBytes);

        // 9. 校验和 (4字节) - 固定值0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        // 更新状态为下一段做准备
        state.textSize = textSize;

        return baos.toByteArray();
    }

    /**
     * extractDeployAddress
     *
     * @brief
     * 功能: 提取部署地址映射: /Module/Payloads/CoreOSPayload/@Base_Address。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractDeployAddress(com.nwpu.verify.entity.Module module) {
        if (module == null || module.getfPayload() == null) {
            long defaultAddress = 0x08000000L;
            return defaultAddress;
        }

        Payload.CoreOSPayload coreOSPayload = module.getfPayload().getfCoreOSPayload();

        // 1. 尝试从CoreOSPayload中获取Base_Address
        if (coreOSPayload != null) {
            long baseAddress = coreOSPayload.getBaseAddress();
            if (baseAddress != 0) {
                return baseAddress;
            }
        }

        // 2. 如果Base_Address不存在或为0，从payloadMemory中获取BaseAddress进行自动分配
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory() != null) {

            CoreOS.HardwareConfiguration.PayloadMemory payloadMemory = module.getfCoreOS().getfHardwareConfiguration()
                    .getfPayloadMemory();

            long payloadBaseAddress = payloadMemory.getfBaseAddress();

            if (payloadBaseAddress != 0) {
                return payloadBaseAddress;
            }
        }

        // 3. 如果都不存在，使用默认值
        long defaultAddress = 0x08000000L;
        return defaultAddress;
    }

    /**
     * extractConfigRecordDeployAddress
     *
     * @brief
     * 功能: 提取 ConfigRecord 段的部署地址映射: /Module/Payloads/ConfigRecordPayload/@Base_Address。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractConfigRecordDeployAddress(com.nwpu.verify.entity.Module module) {
        if (module == null || module.getfPayload() == null) {
            long defaultAddress = 0x08000000L;
            return defaultAddress;
        }

        Payload payload = module.getfPayload();

        // 1. 尝试从 ConfigRecordPayload 列表中获取第一个的 Base_Address
        // 注意：这里假设 configRecord 段对应的是第一个 ConfigRecordPayload
        if (payload.getfConfigRecordPayloads() != null && !payload.getfConfigRecordPayloads().isEmpty()) {
            Payload.ConfigRecordPayload configRecordPayload = payload.getfConfigRecordPayloads().get(0);
            long baseAddress = configRecordPayload.getBaseAddress();
            if (baseAddress != 0) {
                return baseAddress;
            }
        }

        // 2. 如果 Base_Address 不存在或为0，按照 payloads 顺序自动分配
        // 从 payloadMemory 的 BaseAddress 开始累加各个 payload 的大小
        long currentAddress = 0L;

        // 获取 payloadMemory 的起始地址
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory() != null) {

            CoreOS.HardwareConfiguration.PayloadMemory payloadMemory = module.getfCoreOS().getfHardwareConfiguration()
                    .getfPayloadMemory();
            currentAddress = payloadMemory.getfBaseAddress();
        } else {
            currentAddress = 0x08000000L; // 默认起始地址
        }

        // 累加 CoreOSPayload 的大小 (不包含BSS)
        if (payload.getfCoreOSPayload() != null) {
            long coreOSSize = calculateCoreOSSizeWithoutBss(module);
            currentAddress += coreOSSize;
        }

        // 累加所有 SharedLibraryPayload 的大小 (不包含BSS)
        if (payload.getfSharedLibraryPayloads() != null) {
            for (Payload.SharedLibraryPayload slp : payload.getfSharedLibraryPayloads()) {
                String sharedLibName = slp.getfNameRefAsString();
                long sharedLibSize = calculateSharedLibrarySizeWithoutBss(module, sharedLibName);
                currentAddress += sharedLibSize;
            }
        }

        return currentAddress;
    }

    /**
     * calculateCoreOSSizeWithoutBss
     *
     * @brief
     * 功能: 计算CoreOS的大小，不包含BSS段计算公式: MemorySizeData + MemorySizeRoData + MemorySizeText。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculateCoreOSSizeWithoutBss(com.nwpu.verify.entity.Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfMemorySize() != null) {

            CoreOS.MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long dataSize = memorySize.getfMemorySizeData();
            long roDataSize = memorySize.getfMemorySizeRoData();
            long textSize = memorySize.getfMemorySizeText();

            long totalSize = dataSize + roDataSize + textSize;
            System.out.println("    CoreOS section size calculation: Data(0x" + Long.toHexString(dataSize).toUpperCase() +
                    ") + RoData(0x" + Long.toHexString(roDataSize).toUpperCase() +
                    ") + Text(0x" + Long.toHexString(textSize).toUpperCase() +
                    ") = 0x" + Long.toHexString(totalSize).toUpperCase());
            return totalSize;
        }

        System.out.println("    Warning: CoreOS MemorySize does not exist, returning 0");
        return 0L;
    }

    /**
     * generateRodataSegment
     *
     * @brief
     * 功能: 4.2.3.2 分区部署信息.rodata段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateRodataSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Rodata_";

        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节)
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节)
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值0x8
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_RODATA);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - text段ROM部署地址 + text段大小
        long rodataRomAddress = state.textRomAddress + state.textSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(rodataRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);
        System.out.println("  Deploy address: 0x" + Long.toHexString(rodataRomAddress).toUpperCase());

        // 6. 逻辑地址 (4字节，32位 unsigned long) - text段虚拟地址 + text段大小
        long rodataLogicalAddress = state.textLogicalAddress + state.textSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(rodataLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);
        System.out.println("  Logical address: 0x" + Long.toHexString(rodataLogicalAddress).toUpperCase());

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - text段物理地址 + text段大小
        long rodataPhysicalAddress = state.textPhysicalAddress + state.textSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(rodataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);
        System.out.println("  Physical address: 0x" + Long.toHexString(rodataPhysicalAddress).toUpperCase());

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeRoData属性值
        long rodataSize = app.getfMemorySize().getfMemorySizeRoData();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(rodataSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "SegmentSize", segmentSizeBytes);
        System.out.println("  Section size: " + rodataSize + " bytes");

        // 9. 校验和 (4字节)
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        // 更新状态
        state.rodataRomAddress = rodataRomAddress;
        state.rodataLogicalAddress = rodataLogicalAddress;
        state.rodataPhysicalAddress = rodataPhysicalAddress;
        state.rodataSize = rodataSize;

        return baos.toByteArray();
    }

    /**
     * generateDataSegment
     *
     * @brief
     * 功能: 4.2.3.3 分区部署信息.data段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateDataSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Data_";

        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        // 2-4. 域类型、占位符、段类型
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_DATA);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - rodata段ROM部署地址 + rodata段大小
        long dataRomAddress = state.rodataRomAddress + state.rodataSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(dataRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - rodata段虚拟地址 + rodata段大小
        long dataLogicalAddress = state.rodataLogicalAddress + state.rodataSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(dataLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - rodata段物理地址 + rodata段大小
        long dataPhysicalAddress = state.rodataPhysicalAddress + state.rodataSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(dataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeData属性值
        long dataSize = app.getfMemorySize().getfMemorySizeData();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(dataSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "SegmentSize", segmentSizeBytes);

        // 9. 校验和 (4字节)
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        // 更新状态
        state.dataRomAddress = dataRomAddress;
        state.dataLogicalAddress = dataLogicalAddress;
        state.dataPhysicalAddress = dataPhysicalAddress;
        state.dataSize = dataSize;

        return baos.toByteArray();
    }

    /**
     * generatePdataSegment
     *
     * @brief
     * 功能: 4.2.3.4 分区部署信息.pdata段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePdataSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Pdata_";

        // 1-4. 基础字段
        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_PDATA);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - data段ROM部署地址 + data段大小
        long pdataRomAddress = state.dataRomAddress + state.dataSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(pdataRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);
        System.out.println("  Deploy address: 0x" + Long.toHexString(pdataRomAddress).toUpperCase());

        // 6. 逻辑地址 (4字节，32位 unsigned long) - data段虚拟地址 + data段大小
        long pdataLogicalAddress = state.dataLogicalAddress + state.dataSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(pdataLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);
        System.out.println("  Logical address: 0x" + Long.toHexString(pdataLogicalAddress).toUpperCase());

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - data段物理地址 + data段大小
        long pdataPhysicalAddress = state.dataPhysicalAddress + state.dataSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(pdataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);
        System.out.println("  Physical address: 0x" + Long.toHexString(pdataPhysicalAddress).toUpperCase());

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizePersistentData属性值
        long pdataSize = app.getfMemorySize().getfMemorySizePersistentData();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(pdataSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "SegmentSize", segmentSizeBytes);
        System.out.println("  Section size: " + pdataSize + " bytes");

        // 9. 校验和 (4字节)
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        // 更新状态
        state.pdataRomAddress = pdataRomAddress;
        state.pdataLogicalAddress = pdataLogicalAddress;
        state.pdataPhysicalAddress = pdataPhysicalAddress;
        state.pdataSize = pdataSize;

        return baos.toByteArray();
    }

    /**
     * generateBssSegment
     *
     * @brief
     * 功能: 4.2.3.5 分区部署信息.bss段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateBssSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Bss_";

        // 1-4. 基础字段
        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_BSS);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5-7. 地址字段 - 根据.pdata段是否存在来决定
        // 当.pdata段不存在时，取值为.data段地址+.data段大小
        // 当.pdata段存在时，取值为.pdata段地址+.pdata段大小
        long bssRomAddress, bssLogicalAddress, bssPhysicalAddress;

        if (app.getfMemorySize().hasPersistentData()) {
            // .pdata段存在（在XML中已配置），基于.pdata段计算
            bssRomAddress = state.pdataRomAddress + state.pdataSize;
            bssLogicalAddress = state.pdataLogicalAddress + state.pdataSize;
            bssPhysicalAddress = state.pdataPhysicalAddress + state.pdataSize;
        } else {
            // .pdata段不存在（未在XML中配置），基于.data段计算
            bssRomAddress = state.dataRomAddress + state.dataSize;
            bssLogicalAddress = state.dataLogicalAddress + state.dataSize;
            bssPhysicalAddress = state.dataPhysicalAddress + state.dataSize;
        }

        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(bssRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);

        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(bssLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(bssPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeBss属性值
        // PR_21: 修正字段名，使用MemorySizeBss而不是SegmentSize
        long bssSize = app.getfMemorySize().getfMemorySizeBss();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(bssSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "MemorySizeBss", segmentSizeBytes);

        // 9. 校验和 (4字节)
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        // 保存.bss段状态信息供.pbss段使用
        state.bssRomAddress = bssRomAddress;
        state.bssLogicalAddress = bssLogicalAddress;
        state.bssPhysicalAddress = bssPhysicalAddress;
        state.bssSize = bssSize;

        return baos.toByteArray();
    }

    /**
     * generatePbssSegment
     *
     * @brief
     * 功能: 4.2.3.6 分区部署信息.pbss段。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePbssSegment(Partition partition, ApplicationEntity app, com.nwpu.verify.entity.Module module,
            Map<String, byte[]> fieldMap, SegmentStateTracker state) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String partitionName = partition.getfNameAsString();
        String fieldPrefix = "PartitionDeploy_" + partitionName + "_Pbss_";

        // 1-4. 基础字段
        // 1. 域空间名 (32字节) - 来自分区名 /Module/Partitions/Partition/@Name
        String domainSpaceName = partitionName;
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainSpaceName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(fieldPrefix + "DomainName", domainNameBytes);

        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(getDomainType(module, partitionName));
        baos.write(domainTypeBytes);
        fieldMap.put(fieldPrefix + "DomainType", domainTypeBytes);

        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(fieldPrefix + "Placeholder", placeholderBytes);

        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(SEGMENT_TYPE_PBSS);
        baos.write(segmentTypeBytes);
        fieldMap.put(fieldPrefix + "SegmentType", segmentTypeBytes);

        // 5-7. 地址字段 - .pbss段地址计算规则
        // 部署地址：根据.pdata段是否存在来决定
        // - 当未配置.pdata段时，取值为分区的.data段部署地址+分区的.data段大小
        // - 当配置了.pdata段时，取值为分区的.pdata段部署地址+分区的.pdata段大小
        // 逻辑地址：取值为分区的.bss段虚拟地址+分区的.bss段大小
        // 物理地址：取值为分区的.bss段物理地址+分区的.bss段大小

        long pbssRomAddress;
        if (app.getfMemorySize().hasPersistentData()) {
            // .pdata段存在（在XML中已配置），基于.pdata段计算部署地址
            pbssRomAddress = state.pdataRomAddress + state.pdataSize;
        } else {
            // .pdata段不存在（未在XML中配置），基于.data段计算部署地址
            pbssRomAddress = state.dataRomAddress + state.dataSize;
        }

        long pbssLogicalAddress = state.bssLogicalAddress + state.bssSize;
        long pbssPhysicalAddress = state.bssPhysicalAddress + state.bssSize;

        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(pbssRomAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(fieldPrefix + "DeployAddress", deployAddressBytes);

        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesBigEndian(pbssLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(fieldPrefix + "LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Before_PhysicalAddress");

        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(pbssPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(fieldPrefix + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizePersistentBss属性值
        long pbssSize = app.getfMemorySize().getfMemorySizePersistentBss();
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesBigEndian(pbssSize);
        baos.write(segmentSizeBytes);
        fieldMap.put(fieldPrefix + "SegmentSize", segmentSizeBytes);

        // 9. 校验和 (4字节)
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(fieldPrefix + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, fieldPrefix + "Padding_Final");

        return baos.toByteArray();
    }

    /**
     * getPartitionRomBaseAddress
     *
     * @brief
     * 功能: 获取分区的ROM基地址（部署地址）规则：1. 优先从 /Module/Payloads/PartitionPayload/@Base_Address 获取2. 如果Base_Address不存在，则按照新的计算规则：payloadMemory Base_Address + CoreOS相关值(不含BSS) + POS相关值(不含BSS) +kernelConfigRecordRegion Size。
     *
     * @param[in]  partition:      分区对象
     * @param[in]  module:         Module对象
     * @param[in]  partitionIndex: 分区在Module中的索引（用于顺序分配）
     * @return 部署地址
     * @scope internal
     * @errno:
     * N/A
     */
    private long getPartitionRomBaseAddress(Partition partition, com.nwpu.verify.entity.Module module,
            int partitionIndex) {
        // 1. 首先尝试从PartitionPayload中获取Base_Address
        if (module.getfPayload() != null && module.getfPayload().getfPartitionPayloads() != null) {
            for (Payload.PartitionPayload partitionPayload : module.getfPayload().getfPartitionPayloads()) {
                if (partitionPayload.getfNameRefAsString().equals(partition.getfNameAsString())) {
                    long baseAddress = partitionPayload.getBaseAddress();
                    if (baseAddress > 0) {
                        return baseAddress;
                    }
                }
            }
        }

        // 2. 如果Base_Address不存在，使用新的计算规则
        return calculatePartitionDeployAddressWithNewRule(module, partitionIndex);
    }

    /**
     * calculatePartitionDeployAddressWithNewRule
     *
     * @brief
     * 功能: 使用新规则计算分区部署地址新逻辑：1. 在Payloads列表中找到当前分区的位置2.向前查找最近的有Base_Address的Payload（可以是CoreOSPayload、SharedLibraryPayload、ConfigRecordPayload或PartitionPayload）3. 部署地址 = 最近的有Base_Address的Payload地址 + 从那个Payload到当前分区之前所有部分的累计大小。
     *
     * @param[in]  module:         Module对象
     * @param[in]  partitionIndex: 分区索引（在PartitionPayloads列表中的索引）
     * @return 计算得到的部署地址
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculatePartitionDeployAddressWithNewRule(com.nwpu.verify.entity.Module module, int partitionIndex) {
        Payload payload = module.getfPayload();
        if (payload == null) {
            Log.info("[部署地址计算] 错误: Payload为空");
            return 0L;
        }

        Log.info("[部署地址计算] 开始计算分区索引 " + partitionIndex + " 的部署地址");

        // Payloads的顺序：CoreOSPayload -> SharedLibraryPayloads -> ConfigRecordPayloads ->
        // PartitionPayloads
        // 总Payload数 = 1(CoreOS) + SharedLibraryPayloads数量 + ConfigRecordPayloads数量 +
        // 当前分区之前的PartitionPayloads数量

        int coreOSCount = (payload.getfCoreOSPayload() != null) ? 1 : 0;
        int sharedLibCount = (payload.getfSharedLibraryPayloads() != null) ? payload.getfSharedLibraryPayloads().size()
                : 0;
        int configRecordCount = (payload.getfConfigRecordPayloads() != null) ? payload.getfConfigRecordPayloads().size()
                : 0;

        // 当前分区在所有Payload中的位置
        int currentPositionInAllPayloads = coreOSCount + sharedLibCount + configRecordCount + partitionIndex;

        Log.info("[部署地址计算] Payload布局: CoreOS=" + coreOSCount + ", SharedLib=" + sharedLibCount +
                ", ConfigRecord=" + configRecordCount + ", 当前分区位置=" + currentPositionInAllPayloads);

        // 向前查找最近的有Base_Address的Payload
        long lastBaseAddress = 0L;
        long accumulatedSize = 0L;
        String lastPayloadType = "";

        // 1. 检查当前分区之前的所有PartitionPayload（从后往前）
        if (partitionIndex > 0 && payload.getfPartitionPayloads() != null) {
            for (int i = partitionIndex - 1; i >= 0; i--) {
                Payload.PartitionPayload pp = payload.getfPartitionPayloads().get(i);
                // Online分区的Base_Address只表示其在线部署地址，不能作为普通分区自动分配的锚点。
                if (pp.getBaseAddress() > 0 && !pp.isOnline()) {
                    lastBaseAddress = pp.getBaseAddress();
                    lastPayloadType = "PartitionPayload[" + i + "]('" + pp.getfNameRefAsString() + "')";
                    Log.info("[部署地址计算] 找到前置PartitionPayload Base_Address: 0x"
                            + Long.toHexString(lastBaseAddress).toUpperCase() + " (" + pp.getfNameRefAsString() + ")");
                    // 累加从这个Partition到当前分区之前的所有Partition大小
                    for (int j = i; j < partitionIndex; j++) {
                        Payload.PartitionPayload previousPartition = payload.getfPartitionPayloads().get(j);
                        if (previousPartition.isOnline()) {
                            continue;
                        }
                        String partName = previousPartition.getfNameRefAsString();
                        long partSize = getPartitionSize(module, partName);
                        accumulatedSize += partSize;
                        Log.info("[部署地址计算]   + Partition[" + j + "](" + partName + ") 大小: 0x"
                                + Long.toHexString(partSize).toUpperCase());
                    }
                    long result = lastBaseAddress + accumulatedSize;
                    Log.info("[部署地址计算] ===== 最终地址: 0x" + Long.toHexString(result).toUpperCase() + " = "
                            + lastPayloadType + "(0x" + Long.toHexString(lastBaseAddress).toUpperCase() + ") + 累计大小(0x"
                            + Long.toHexString(accumulatedSize).toUpperCase() + ") =====");
                    return result;
                }
            }
            // 如果前面的Partition都没有Base_Address，累加它们的大小
            for (int i = 0; i < partitionIndex; i++) {
                Payload.PartitionPayload previousPartition = payload.getfPartitionPayloads().get(i);
                if (previousPartition.isOnline()) {
                    continue;
                }
                String partName = previousPartition.getfNameRefAsString();
                long partSize = getPartitionSize(module, partName);
                accumulatedSize += partSize;
                Log.info("[部署地址计算]   累加Partition[" + i + "](" + partName + ") 大小: 0x"
                        + Long.toHexString(partSize).toUpperCase());
            }
        }

        // 2. 检查ConfigRecordPayloads（从后往前）
        if (payload.getfConfigRecordPayloads() != null && !payload.getfConfigRecordPayloads().isEmpty()) {
            for (int i = payload.getfConfigRecordPayloads().size() - 1; i >= 0; i--) {
                Payload.ConfigRecordPayload crp = payload.getfConfigRecordPayloads().get(i);
                if (crp.getBaseAddress() > 0) {
                    lastBaseAddress = crp.getBaseAddress();
                    lastPayloadType = "ConfigRecordPayload('" + crp.getfNameRefAsString() + "')";
                    Log.info("[部署地址计算] 找到ConfigRecordPayload Base_Address: 0x"
                            + Long.toHexString(lastBaseAddress).toUpperCase() + " (" + crp.getfNameRefAsString() + ")");
                    // 重置accumulatedSize，因为找到了Base_Address
                    accumulatedSize = 0L;
                    // 累加从这个ConfigRecord到当前分区之前的所有ConfigRecord大小
                    // ConfigRecord的大小需要从kernelConfigRecordRegion读取
                    for (int j = i; j < payload.getfConfigRecordPayloads().size(); j++) {
                        long crSize = getConfigRecordSize(module);
                        accumulatedSize += crSize;
                        Log.info("[部署地址计算]   + ConfigRecord[" + j + "] 大小(从kernelConfigRecordRegion): 0x"
                                + Long.toHexString(crSize).toUpperCase());
                    }
                    // 累加从 ConfigRecord 之后到当前分区之前的所有 Partition 大小
                    for (int j = 0; j < partitionIndex; j++) {
                        Payload.PartitionPayload previousPartition = payload.getfPartitionPayloads().get(j);
                        if (previousPartition.isOnline()) {
                            continue;
                        }
                        String partName = previousPartition.getfNameRefAsString();
                        long partSize = getPartitionSize(module, partName);
                        accumulatedSize += partSize;
                        Log.info("[部署地址计算]   + Partition[" + j + "](" + partName + ") 大小: 0x"
                                + Long.toHexString(partSize).toUpperCase());
                    }
                    long result = lastBaseAddress + accumulatedSize;
                    Log.info("[部署地址计算] ===== 最终地址: 0x" + Long.toHexString(result).toUpperCase() + " = "
                            + lastPayloadType + "(0x" + Long.toHexString(lastBaseAddress).toUpperCase() + ") + 累计大小(0x"
                            + Long.toHexString(accumulatedSize).toUpperCase() + ") =====");
                    return result;
                }
            }
            // 如果所有ConfigRecord都没有Base_Address，累加它们的大小
            for (int i = 0; i < payload.getfConfigRecordPayloads().size(); i++) {
                long crSize = getConfigRecordSize(module);
                accumulatedSize += crSize;
                Log.info("[部署地址计算]   累加ConfigRecord[" + i + "] 大小(从kernelConfigRecordRegion): 0x"
                        + Long.toHexString(crSize).toUpperCase());
            }
        }

        // 3. 检查SharedLibraryPayloads（从后往前）
        if (payload.getfSharedLibraryPayloads() != null && !payload.getfSharedLibraryPayloads().isEmpty()) {
            for (int i = payload.getfSharedLibraryPayloads().size() - 1; i >= 0; i--) {
                Payload.SharedLibraryPayload slp = payload.getfSharedLibraryPayloads().get(i);
                if (slp.getBaseAddress() > 0) {
                    lastBaseAddress = slp.getBaseAddress();
                    lastPayloadType = "SharedLibraryPayload('" + slp.getfNameRefAsString() + "')";
                    Log.info("[部署地址计算] 找到SharedLibraryPayload Base_Address: 0x"
                            + Long.toHexString(lastBaseAddress).toUpperCase() + " (" + slp.getfNameRefAsString() + ")");
                    // 重置accumulatedSize，因为找到了Base_Address
                    accumulatedSize = 0L;
                    // 累加从这个SharedLibrary到当前分区之前的所有SharedLibrary大小
                    for (int j = i; j < payload.getfSharedLibraryPayloads().size(); j++) {
                        String slName = payload.getfSharedLibraryPayloads().get(j).getfNameRefAsString();
                        long slSize = getSharedLibrarySize(module, slName);
                        accumulatedSize += slSize;
                        Log.info("[部署地址计算]   + SharedLibrary[" + j + "](" + slName + ") 大小: 0x"
                                + Long.toHexString(slSize).toUpperCase());
                    }
                    // 累加从 SharedLibrary 之后到当前分区之前的所有 ConfigRecord 大小
                    if (payload.getfConfigRecordPayloads() != null) {
                        for (int j = 0; j < payload.getfConfigRecordPayloads().size(); j++) {
                            long crSize = getConfigRecordSize(module);
                            accumulatedSize += crSize;
                            Log.info("[部署地址计算]   + ConfigRecord[" + j + "] 大小: 0x"
                                    + Long.toHexString(crSize).toUpperCase());
                        }
                    }
                    // 累加从 SharedLibrary 之后到当前分区之前的所有 Partition 大小
                    for (int j = 0; j < partitionIndex; j++) {
                        Payload.PartitionPayload previousPartition = payload.getfPartitionPayloads().get(j);
                        if (previousPartition.isOnline()) {
                            continue;
                        }
                        String partName = previousPartition.getfNameRefAsString();
                        long partSize = getPartitionSize(module, partName);
                        accumulatedSize += partSize;
                        Log.info("[部署地址计算]   + Partition[" + j + "](" + partName + ") 大小: 0x"
                                + Long.toHexString(partSize).toUpperCase());
                    }
                    long result = lastBaseAddress + accumulatedSize;
                    Log.info("[部署地址计算] ===== 最终地址: 0x" + Long.toHexString(result).toUpperCase() + " = "
                            + lastPayloadType + "(0x" + Long.toHexString(lastBaseAddress).toUpperCase() + ") + 累计大小(0x"
                            + Long.toHexString(accumulatedSize).toUpperCase() + ") =====");
                    return result;
                }
            }
            // 如果所有SharedLibrary都没有Base_Address，累加它们的大小
            for (int i = 0; i < payload.getfSharedLibraryPayloads().size(); i++) {
                String slName = payload.getfSharedLibraryPayloads().get(i).getfNameRefAsString();
                long slSize = getSharedLibrarySize(module, slName);
                accumulatedSize += slSize;
                Log.info("[部署地址计算]   累加SharedLibrary[" + i + "](" + slName + ") 大小: 0x"
                        + Long.toHexString(slSize).toUpperCase());
            }
        }

        // 4. 检查CoreOSPayload
        if (payload.getfCoreOSPayload() != null && payload.getfCoreOSPayload().getBaseAddress() > 0) {
            lastBaseAddress = payload.getfCoreOSPayload().getBaseAddress();
            lastPayloadType = "CoreOSPayload";
            Log.info("[部署地址计算] 找到CoreOSPayload Base_Address: 0x" + Long.toHexString(lastBaseAddress).toUpperCase());
            // 重置accumulatedSize，因为找到了Base_Address
            accumulatedSize = 0L;
            long coreOSSize = payload.getfCoreOSPayload().getSize();
            accumulatedSize += coreOSSize;
            Log.info("[部署地址计算]   + CoreOS 大小: 0x" + Long.toHexString(coreOSSize).toUpperCase());
            // 累加从 CoreOS 之后到当前分区之前的所有 SharedLibrary 大小
            if (payload.getfSharedLibraryPayloads() != null) {
                for (int j = 0; j < payload.getfSharedLibraryPayloads().size(); j++) {
                    String slName = payload.getfSharedLibraryPayloads().get(j).getfNameRefAsString();
                    long slSize = getSharedLibrarySize(module, slName);
                    accumulatedSize += slSize;
                    Log.info("[部署地址计算]   + SharedLibrary[" + j + "](" + slName + ") 大小: 0x"
                            + Long.toHexString(slSize).toUpperCase());
                }
            }
            // 累加从 CoreOS 之后到当前分区之前的所有 ConfigRecord 大小
            if (payload.getfConfigRecordPayloads() != null) {
                for (int j = 0; j < payload.getfConfigRecordPayloads().size(); j++) {
                    long crSize = getConfigRecordSize(module);
                    accumulatedSize += crSize;
                    Log.info("[部署地址计算]   + ConfigRecord[" + j + "] 大小: 0x" + Long.toHexString(crSize).toUpperCase());
                }
            }
            // 累加从 CoreOS 之后到当前分区之前的所有 Partition 大小
            for (int j = 0; j < partitionIndex; j++) {
                Payload.PartitionPayload previousPartition = payload.getfPartitionPayloads().get(j);
                if (previousPartition.isOnline()) {
                    continue;
                }
                String partName = previousPartition.getfNameRefAsString();
                long partSize = getPartitionSize(module, partName);
                accumulatedSize += partSize;
                Log.info("[部署地址计算]   + Partition[" + j + "](" + partName + ") 大小: 0x"
                        + Long.toHexString(partSize).toUpperCase());
            }
            long result = lastBaseAddress + accumulatedSize;
            Log.info("[部署地址计算] ===== 最终地址: 0x" + Long.toHexString(result).toUpperCase() + " = " + lastPayloadType
                    + "(0x" + Long.toHexString(lastBaseAddress).toUpperCase() + ") + 累计大小(0x"
                    + Long.toHexString(accumulatedSize).toUpperCase() + ") =====");
            return result;
        }

        // 5. 如果都没有Base_Address，使用CoreOS的payloadMemory Base_Address作为基地址
        // 需要先累加CoreOS的大小（从CoreOS.MemorySize计算，不含BSS）
        if (module.getfCoreOS() != null && module.getfCoreOS().getfMemorySize() != null) {
            CoreOS.MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long textSize = memorySize.getfMemorySizeText();
            long roDataSize = memorySize.getfMemorySizeRoData();
            long dataSize = memorySize.getfMemorySizeData();
            long coreOSSize = textSize + roDataSize + dataSize;
            accumulatedSize += coreOSSize;
            Log.info("[部署地址计算]   累加CoreOS 大小: 0x" + Long.toHexString(coreOSSize).toUpperCase() +
                    " (Text=0x" + Long.toHexString(textSize).toUpperCase() +
                    ", RoData=0x" + Long.toHexString(roDataSize).toUpperCase() +
                    ", Data=0x" + Long.toHexString(dataSize).toUpperCase() + ")");
        }

        long payloadMemoryBaseAddress = 0L;
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory() != null) {
            payloadMemoryBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory()
                    .getfBaseAddress();
            if (payloadMemoryBaseAddress > 0) {
                Log.info("[部署地址计算] 使用CoreOS的payloadMemory Base_Address: 0x"
                        + Long.toHexString(payloadMemoryBaseAddress).toUpperCase());
            }
        }

        long result = payloadMemoryBaseAddress + accumulatedSize;
        Log.info("[部署地址计算] ===== 最终地址: 0x" + Long.toHexString(result).toUpperCase() + " = payloadMemory Base_Address(0x"
                + Long.toHexString(payloadMemoryBaseAddress).toUpperCase() + ") + 累计大小(0x"
                + Long.toHexString(accumulatedSize).toUpperCase() + ") =====");
        return result;
    }

    /**
     * calculateSharedLibrarySizeWithoutBss
     *
     * @brief
     * 功能: 计算SharedLibrary的大小，不包含BSS段计算公式: MemorySizeData + MemorySizeRoData + MemorySizeText +MemorySizePersistentData。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculateSharedLibrarySizeWithoutBss(com.nwpu.verify.entity.Module module, String sharedLibName) {
        if (module != null && module.getfSharedLibraries() != null) {
            SharedLibrary sharedLib = module.getfSharedLibraries().get(sharedLibName);
            if (sharedLib != null && sharedLib.getfMemorySize() != null) {
                SharedLibrary.MemorySize memorySize = sharedLib.getfMemorySize();

                long dataSize = parseHexString(memorySize.getfMemorySizeData());
                long roDataSize = parseHexString(memorySize.getfMemorySizeRoData());
                long textSize = parseHexString(memorySize.getfMemorySizeText());
                long persistentDataSize = parseHexString(memorySize.getfMemorySizePersistentData());

                long totalSize = dataSize + roDataSize + textSize + persistentDataSize;
                Log.info("[部署地址计算]   " + sharedLibName + "段大小明细: Text=0x" + Long.toHexString(textSize).toUpperCase() +
                        ", RoData=0x" + Long.toHexString(roDataSize).toUpperCase() +
                        ", Data=0x" + Long.toHexString(dataSize).toUpperCase() +
                        ", PersistentData=0x" + Long.toHexString(persistentDataSize).toUpperCase() +
                        ", 总计=0x" + Long.toHexString(totalSize).toUpperCase());
                return totalSize;
            }
        }

        return 0L;
    }

    /**
     * parseHexString
     *
     * @brief
     * 功能: 解析十六进制字符串为long值支持格式: "0x12345" 或 "12345"。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long parseHexString(String hexStr) {
        if (hexStr == null || hexStr.trim().isEmpty()) {
            return 0L;
        }

        String trimmed = hexStr.trim();
        if (trimmed.toLowerCase().startsWith("0x")) {
            trimmed = trimmed.substring(2);
        }

        try {
            return Long.parseLong(trimmed, 16);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * getSharedLibrarySize
     *
     * @brief
     * 功能: 获取SharedLibrary的实际大小SharedLibrary的大小需要从其MemorySize计算（text+rodata+data+persistentData，不含BSS）。
     *
     * @param[in]  module:        Module对象
     * @param[in]  sharedLibName: SharedLibrary名称
     * @return SharedLibrary大小
     * @scope internal
     * @errno:
     * N/A
     */
    private long getSharedLibrarySize(com.nwpu.verify.entity.Module module, String sharedLibName) {
        if (module.getfSharedLibraries() != null) {
            SharedLibrary sharedLib = module.getfSharedLibraries().get(sharedLibName);
            if (sharedLib != null && sharedLib.getfMemorySize() != null) {
                SharedLibrary.MemorySize memorySize = sharedLib.getfMemorySize();
                long textSize = parseHexString(memorySize.getfMemorySizeText());
                long roDataSize = parseHexString(memorySize.getfMemorySizeRoData());
                long dataSize = parseHexString(memorySize.getfMemorySizeData());
                long persistentDataSize = parseHexString(memorySize.getfMemorySizePersistentData());
                return textSize + roDataSize + dataSize + persistentDataSize;
            }
        }
        Log.info("[部署地址计算] 警告: 无法获取SharedLibrary '" + sharedLibName + "' 的大小");
        return 0L;
    }

    /**
     * getPartitionIndexInPayloads
     *
     * @brief
     * 功能: 获取分区在Payloads中的索引。
     *
     * @param[in]  module:        Module对象
     * @param[in]  partitionName: 分区名称
     * @return 分区在PartitionPayloads中的索引，如果找不到返回-1
     * @scope internal
     * @errno:
     * N/A
     */
    private int getPartitionIndexInPayloads(com.nwpu.verify.entity.Module module, String partitionName) {
        if (module.getfPayload() == null || module.getfPayload().getfPartitionPayloads() == null) {
            return -1;
        }

        List<Payload.PartitionPayload> partitionPayloads = module.getfPayload().getfPartitionPayloads();
        for (int i = 0; i < partitionPayloads.size(); i++) {
            if (partitionPayloads.get(i).getfNameRefAsString().equals(partitionName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * getPartitionSize
     *
     * @brief
     * 功能: 获取Partition的实际大小Partition的大小需要从对应Application的MemorySize计算（text+rodata+data+persistentData，不含BSS）。
     *
     * @param[in]  module:        Module对象
     * @param[in]  partitionName: Partition名称
     * @return Partition大小
     * @scope internal
     * @errno:
     * N/A
     */
    private long getPartitionSize(com.nwpu.verify.entity.Module module, String partitionName) {
        // 从Module.Partitions中查找对应的Partition
        Partition partition = module.getfPartitions().get(partitionName);
        if (partition != null) {
            // 获取Partition对应的Application
            ApplicationEntity app = module.getfApplications().get(partition.getfApplicationNameRef());
            if (app != null && app.getfMemorySize() != null) {
                return calculatePartitionSizeWithoutBss(app);
            }
        }
        Log.info("[部署地址计算] 警告: 无法获取Partition '" + partitionName + "' 的大小");
        return 0L;
    }

    /**
     * getConfigRecordSize
     *
     * @brief
     * 功能: 获取ConfigRecord的大小ConfigRecord的大小需要从coreOS.xml的kernelConfigRecordRegion Size读取。
     *
     * @param[in]  module: Module对象
     * @return ConfigRecord大小
     * @scope internal
     * @errno:
     * N/A
     */
    private long getConfigRecordSize(com.nwpu.verify.entity.Module module) {
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            String configRecordSizeStr = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfKernelConfigRecordRegionSize();
            if (configRecordSizeStr != null && !configRecordSizeStr.trim().isEmpty()) {
                try {
                    if (configRecordSizeStr.trim().toLowerCase().startsWith("0x")) {
                        // 十六进制格式
                        return Long.parseLong(configRecordSizeStr.trim().substring(2), 16);
                    } else {
                        // 十进制格式
                        return Long.parseLong(configRecordSizeStr.trim());
                    }
                } catch (NumberFormatException e) {
                    Log.info("[部署地址计算] 警告: 无法解析kernelConfigRecordRegion Size: " + configRecordSizeStr);
                    return 0L;
                }
            }
        }
        Log.info("[部署地址计算] 警告: 无法获取kernelConfigRecordRegion Size");
        return 0L;
    }



    /**
     * getCurrentPartitionName
     *
     * @brief
     * 功能: 获取当前分区的名称。
     *
     * @param[in]  module:         Module对象
     * @param[in]  partitionIndex: 分区索引
     * @return 分区名称，如果找不到返回null
     * @scope internal
     * @errno:
     * N/A
     */
    private String getCurrentPartitionName(com.nwpu.verify.entity.Module module, int partitionIndex) {
        if (module.getfPartitions() == null) {
            return null;
        }

        int index = 0;
        for (Partition partition : module.getfPartitions().values()) {
            if (index == partitionIndex) {
                return partition.getfNameAsString();
            }
            index++;
        }
        return null;
    }

    /**
     * calculatePartitionSizeWithoutBss
     *
     * @brief
     * 功能: 计算分区大小（不含BSS段）。
     *
     * @param[in]  app: 应用程序对象
     * @return 分区大小（text + rodata + data + persistentData）
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculatePartitionSizeWithoutBss(ApplicationEntity app) {
        long size = 0L;
        if (app.getfMemorySize() != null) {
            size += app.getfMemorySize().getfMemorySizeText();
            size += app.getfMemorySize().getfMemorySizeRoData();
            size += app.getfMemorySize().getfMemorySizeData();
            size += app.getfMemorySize().getfMemorySizePersistentData();
            // 注意：不包含BSS和PersistentBss段
        }
        return size;
    }



    /**
     * 分区物理地址跟踪器
     * 用于管理分区物理地址的分配：
     * - 第一个分区：userMemoryRegion基地址 + 所有共享数据的大小
     * - 后续分区：前一个分区的物理地址 + 前一个分区的RequiredMemorySize
     */
    private static class PartitionPhysicalAddressTracker {
        private long lastPartitionPhysicalAddress;
        private Partition lastPartition;
        private final long totalSharedDataSize;
        private final long userMemoryBaseAddress;

        public PartitionPhysicalAddressTracker(com.nwpu.verify.entity.Module module) {
            this.lastPartitionPhysicalAddress = 0;
            this.lastPartition = null;

            // 获取userMemoryRegion基地址
            if (module.getfCoreOS() != null &&
                    module.getfCoreOS().getfHardwareConfiguration() != null &&
                    module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion() != null) {
                this.userMemoryBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion()
                        .getfBaseAddress();
            } else {
                this.userMemoryBaseAddress = 0L;
                System.err.println("Warning: cannot get userMemoryRegion base address");
            }

            // 计算所有共享数据的总大小
            this.totalSharedDataSize = calculateTotalSharedDataSize(module);
            System.out.println("Total size of all shared data: " + totalSharedDataSize + " (0x"
                    + Long.toHexString(totalSharedDataSize).toUpperCase() + ")");
        }

        /**
         * calculateTotalSharedDataSize
         *
         * @brief
         * 功能: 计算所有共享数据的总大小。
         * @return N/A
         * @scope internal
         * @errno:
         * N/A
         */
        private long calculateTotalSharedDataSize(com.nwpu.verify.entity.Module module) {
            long total = 0;
            if (module.getfSharedDatas() != null) {
                for (com.nwpu.verify.entity.SharedData sharedData : module.getfSharedDatas().values()) {
                    if (sharedData != null && sharedData.getfSharedDataDescription() != null) {
                        total += sharedData.getfSharedDataDescription().getSizeAsLong();
                    }
                }
            }
            return total;
        }

        /**
         * getPhysicalAddressForPartition
         *
         * @brief
         * 功能: 获取指定分区的物理地址。
         *
         * @param[in]  partition:      分区对象
         * @param[in]  partitionIndex: 分区索引（从0开始）
         * @return 物理地址
         * @scope external
         * @errno:
         * N/A
         */
        public long getPhysicalAddressForPartition(Partition partition, int partitionIndex) {
            if (partitionIndex == 0) {
                // 第一个分区：userMemoryRegion基地址 + 所有共享数据的大小
                long physicalAddress = userMemoryBaseAddress + totalSharedDataSize;
                this.lastPartitionPhysicalAddress = physicalAddress;
                System.out.println("  First partition physical address = userMemory base address(0x" +
                        Long.toHexString(userMemoryBaseAddress).toUpperCase() + ") + total shared data size(0x" +
                        Long.toHexString(totalSharedDataSize).toUpperCase() + ") = 0x" +
                        Long.toHexString(physicalAddress).toUpperCase());
                return physicalAddress;
            } else {
                // 后续分区：前一个分区的物理地址 + 前一个分区的RequiredMemorySize
                if (lastPartition == null || lastPartition.getfSettings() == null) {
                    System.err.println("Warning: cannot get previous partition info, using lastPartitionPhysicalAddress");
                    return lastPartitionPhysicalAddress;
                }

                long requiredMemorySize = lastPartition.getfSettings().getfRequiredMemorySize();
                long physicalAddress = lastPartitionPhysicalAddress + requiredMemorySize;
                this.lastPartitionPhysicalAddress = physicalAddress;
                System.out.println("  Subsequent partition physical address = previous partition physical address(0x" +
                        Long.toHexString(lastPartitionPhysicalAddress - requiredMemorySize).toUpperCase() +
                        ") + previous partition RequiredMemorySize(0x" +
                        Long.toHexString(requiredMemorySize).toUpperCase() + ") = 0x" +
                        Long.toHexString(physicalAddress).toUpperCase());
                return physicalAddress;
            }
        }

        /**
         * updateLastPartition
         *
         * @brief
         * 功能: 更新最后一个分区信息（在处理完一个分区后调用）。
         * @return N/A
         * @scope external
         * @errno:
         * N/A
         */
        public void updateLastPartition(Partition partition) {
            this.lastPartition = partition;
        }
    }

    /**
     * 段状态跟踪器 - 用于跟踪各段的地址和大小
     */
    private static class SegmentStateTracker {
        // .text段
        long textRomAddress;
        long textLogicalAddress;
        long textPhysicalAddress;
        long textSize;

        // .rodata段
        long rodataRomAddress;
        long rodataLogicalAddress;
        long rodataPhysicalAddress;
        long rodataSize;

        // .data段
        long dataRomAddress;
        long dataLogicalAddress;
        long dataPhysicalAddress;
        long dataSize;

        // .pdata段
        long pdataRomAddress;
        long pdataLogicalAddress;
        long pdataPhysicalAddress;
        long pdataSize;

        // .bss段
        long bssRomAddress;
        long bssLogicalAddress;
        long bssPhysicalAddress;
        long bssSize;
    }

    /**
     * add8ByteAlignmentIfNeeded
     *
     * @brief
     * 功能: 在8字节字段前检查是否需要padding来达到8字节对齐如果需要，添加padding。
     *
     * @param[in]  baos:        字节流
     * @param[in]  fieldMap:    字段映射
     * @param[in]  paddingName: padding字段名称
     * @throws IOException IO异常
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
}
