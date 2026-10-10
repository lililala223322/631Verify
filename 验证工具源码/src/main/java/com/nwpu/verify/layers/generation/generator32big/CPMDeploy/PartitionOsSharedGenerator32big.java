/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionOsSharedGenerator32big.java
 * 
 * 描述    : 生成PartitionOsShared相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMDeploy;

import com.nwpu.verify.entity.CoreOS;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.SharedLibrary;
import com.nwpu.verify.entity.Payload;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 分区操作系统共享库部署信息配置生成器 (32位大端)负责生成分区操作系统共享库部署信息配置存储结构的二进制内容已实现：.text段、.rodata段、.data段、.pdata段、.bss段、.pbss段。
 */
public class PartitionOsSharedGenerator32big {

    // 固定值常量
    private static final int DOMAIN_TYPE = 1;                    // 域类型固定值
    private static final int PLACEHOLDER = 0;                   // 占位符固定值
    private static final int TEXT_SEGMENT_TYPE = 0x1;           // .text段类型固定值
    private static final int RODATA_SEGMENT_TYPE = 0x8;         // .rodata段类型固定值
    private static final int DATA_SEGMENT_TYPE = 0x2;           // .data段类型固定值
    private static final int PDATA_SEGMENT_TYPE = 0x10;         // .pdata段类型固定值
    private static final int BSS_SEGMENT_TYPE = 0x4;            // .bss段类型固定值
    private static final int PBSS_SEGMENT_TYPE = 0x20;          // .pbss段类型固定值
    private static final int CHECKSUM = 0x1234;                 // 校验和固定值
    
    /**
     * 段地址和大小信息（用于链式计算）
     */
    private static class SegmentInfo {
        long deployAddress;
        long logicalAddress;
        long physicalAddress;
        long size;
        
        SegmentInfo(long deployAddress, long logicalAddress, long physicalAddress, long size) {
            this.deployAddress = deployAddress;
            this.logicalAddress = logicalAddress;
            this.physicalAddress = physicalAddress;
            this.size = size;
        }
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成分区操作系统共享库部署信息配置的二进制内容。
     *
     * @param[in]  module: 包含配置信息的Module对象
     * @param[in]  fieldMap: 用于记录字段映射
     * @return 二进制字节数组
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if (module.getfSharedLibraries() == null || module.getfSharedLibraries().isEmpty()) {
            return baos.toByteArray(); // 如果没有共享库，则返回空字节数组
        }

        // 从XML获取LibRegion基地址
        long libRegionBaseAddress = extractLibRegionBaseAddress(module);
        
        // 维护当前物理地址，用于计算每个共享库的.text段物理地址
        long currentPhysicalAddress = libRegionBaseAddress;

        int sharedLibraryIndex = 0;
        for (Map.Entry<String, SharedLibrary> entry : module.getfSharedLibraries().entrySet()) {
            SharedLibrary sharedLibrary = entry.getValue();
            if (sharedLibrary == null) {
                continue;
            }

            // 生成.text段配置并保存段信息
            SegmentInfo[] segmentInfoHolder = new SegmentInfo[1];
            byte[] textSegmentBytes = generateTextSegment(module, sharedLibrary, currentPhysicalAddress, 
                                                        sharedLibraryIndex, fieldMap, segmentInfoHolder);
            baos.write(textSegmentBytes);
            SegmentInfo textInfo = segmentInfoHolder[0];

            // 生成.rodata段配置（基于.text段）
            byte[] rodataSegmentBytes = generateRodataSegment(module, sharedLibrary, libRegionBaseAddress, 
                                                            sharedLibraryIndex, fieldMap, textInfo, segmentInfoHolder);
            baos.write(rodataSegmentBytes);
            SegmentInfo rodataInfo = segmentInfoHolder[0];

            // 生成.data段配置（基于.rodata段）
            byte[] dataSegmentBytes = generateDataSegment(module, sharedLibrary, libRegionBaseAddress, 
                                                        sharedLibraryIndex, fieldMap, rodataInfo, segmentInfoHolder);
            baos.write(dataSegmentBytes);
            SegmentInfo dataInfo = segmentInfoHolder[0];

            // 生成.pdata段配置（仅当存在MemorySizePersistentData时，基于.data段）
            SegmentInfo lastSegmentInfo = dataInfo; // 跟踪最后一个段（data或pdata）
            if (sharedLibrary.getfMemorySize() != null && 
                sharedLibrary.getfMemorySize().getfMemorySizePersistentData() != null &&
                !sharedLibrary.getfMemorySize().getfMemorySizePersistentData().trim().isEmpty()) {
                
                byte[] pdataSegmentBytes = generatePdataSegment(module, sharedLibrary, libRegionBaseAddress, 
                                                              sharedLibraryIndex, fieldMap, dataInfo, segmentInfoHolder);
                baos.write(pdataSegmentBytes);
                lastSegmentInfo = segmentInfoHolder[0];
            }

            // 生成.bss段配置（基于最后一个段：data或pdata）
            byte[] bssSegmentBytes = generateBssSegment(module, sharedLibrary, libRegionBaseAddress, 
                                                      sharedLibraryIndex, fieldMap, lastSegmentInfo, segmentInfoHolder);
            baos.write(bssSegmentBytes);
            SegmentInfo bssInfo = segmentInfoHolder[0];

            //生成.pbss段配置（仅当存在MemorySizePersistentBss时）
            // 部署地址基于.data或.pdata段，逻辑地址和物理地址基于.bss段
            SegmentInfo lastPhysicalSegmentInfo = bssInfo; // 跟踪最后一个段的物理信息
            if (sharedLibrary.getfMemorySize() != null && 
                sharedLibrary.getfMemorySize().getfMemorySizePersistentBss() != null &&
                !sharedLibrary.getfMemorySize().getfMemorySizePersistentBss().trim().isEmpty()) {
                
                byte[] pbssSegmentBytes = generatePbssSegment(module, sharedLibrary, libRegionBaseAddress, 
                                                            sharedLibraryIndex, fieldMap, lastSegmentInfo, bssInfo, segmentInfoHolder);
                baos.write(pbssSegmentBytes);
                lastPhysicalSegmentInfo = segmentInfoHolder[0];
            }
            
            // 更新下一个共享库的物理地址起始位置
            currentPhysicalAddress = lastPhysicalSegmentInfo.physicalAddress + lastPhysicalSegmentInfo.size;

            sharedLibraryIndex++;
        }

        return baos.toByteArray();
    }

    /**
     * generateTextSegment
     *
     * @brief
     * 功能: 生成.text段配置结构。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @param[in]  segmentInfoHolder: 用于返回段信息的数组（长度为1）
     * @return .text段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generateTextSegment(Module module, SharedLibrary sharedLibrary, 
                                            long currentPhysicalAddress, int index, 
                                            Map<String, byte[]> fieldMap, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Text_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x1"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(TEXT_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);
        
        // 5. 部署地址 (4字节，32位 unsigned long) - 从SharedLibraryPayload的Base_Address获取
        long deployAddress = getSharedLibraryBaseAddress(module, sharedLibrary);
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(deployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - VirtualAddress属性值
        long virtualAddress = sharedLibrary.getfVirtualAddress();
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(virtualAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节，unsigned long long) - 当前共享库的物理地址起始位置
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(currentPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeText属性值
        long textSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeText());
        byte[] textSizeBytes = BinaryUtils.longTo4BytesBigEndian(textSize);
        baos.write(textSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", textSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");

        // 保存段信息供后续段使用
        segmentInfoHolder[0] = new SegmentInfo(deployAddress, virtualAddress, currentPhysicalAddress, textSize);

        return baos.toByteArray();
    }

    /**
     * generateRodataSegment
     *
     * @brief
     * 功能: 生成.rodata段配置结构。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @param[in]  textInfo: .text段信息
     * @param[in]  segmentInfoHolder: 用于返回段信息的数组（长度为1）
     * @return .rodata段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generateRodataSegment(Module module, SharedLibrary sharedLibrary, 
                                              long libRegionBaseAddress, int index, 
                                              Map<String, byte[]> fieldMap, SegmentInfo textInfo, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Rodata_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x8"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(RODATA_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - .text段部署地址 + .text段大小
        long rodataDeployAddress = textInfo.deployAddress + textInfo.size;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(rodataDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - .text段逻辑地址 + .text段大小
        long rodataLogicalAddress = textInfo.logicalAddress + textInfo.size;
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(rodataLogicalAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);
        
        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - .text段物理地址 + .text段大小
        long rodataPhysicalAddress = textInfo.physicalAddress + textInfo.size;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(rodataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeRoData属性值
        long rodataSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeRoData());
        byte[] rodataSizeBytes = BinaryUtils.longTo4BytesBigEndian(rodataSize);
        baos.write(rodataSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", rodataSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");

        // 保存段信息供后续段使用
        segmentInfoHolder[0] = new SegmentInfo(rodataDeployAddress, rodataLogicalAddress, rodataPhysicalAddress, rodataSize);

        return baos.toByteArray();
    }

    /**
     * generateDataSegment
     *
     * @brief
     * 功能: 生成.data段配置结构。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @return .data段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generateDataSegment(Module module, SharedLibrary sharedLibrary, 
                                            long libRegionBaseAddress, int index, 
                                            Map<String, byte[]> fieldMap, SegmentInfo rodataInfo, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Data_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x2"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(DATA_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - .rodata段部署地址 + .rodata段大小
        long dataDeployAddress = rodataInfo.deployAddress + rodataInfo.size;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(dataDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - .rodata段逻辑地址 + .rodata段大小
        long dataLogicalAddress = rodataInfo.logicalAddress + rodataInfo.size;
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(dataLogicalAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - .rodata段物理地址 + .rodata段大小
        long dataPhysicalAddress = rodataInfo.physicalAddress + rodataInfo.size;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(dataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeData属性值
        long dataSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeData());
        byte[] dataSizeBytes = BinaryUtils.longTo4BytesBigEndian(dataSize);
        baos.write(dataSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", dataSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");

        // 保存段信息供后续段使用
        segmentInfoHolder[0] = new SegmentInfo(dataDeployAddress, dataLogicalAddress, dataPhysicalAddress, dataSize);

        return baos.toByteArray();
    }

    /**
     * generatePdataSegment
     *
     * @brief
     * 功能: 生成.pdata段配置结构仅当分区操作系统共享库存在MemorySizePersistentData属性时才生成。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @return .pdata段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generatePdataSegment(Module module, SharedLibrary sharedLibrary, 
                                             long libRegionBaseAddress, int index, 
                                             Map<String, byte[]> fieldMap, SegmentInfo dataInfo, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Pdata_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x10"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(PDATA_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - .data段部署地址 + .data段大小
        long pdataDeployAddress = dataInfo.deployAddress + dataInfo.size;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(pdataDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - .data段逻辑地址 + .data段大小
        long pdataLogicalAddress = dataInfo.logicalAddress + dataInfo.size;
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(pdataLogicalAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - .data段物理地址 + .data段大小
        long pdataPhysicalAddress = dataInfo.physicalAddress + dataInfo.size;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(pdataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizePersistentData属性值
        long pdataSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizePersistentData());
        byte[] pdataSizeBytes = BinaryUtils.longTo4BytesBigEndian(pdataSize);
        baos.write(pdataSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", pdataSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");

        // 保存段信息供后续段使用
        segmentInfoHolder[0] = new SegmentInfo(pdataDeployAddress, pdataLogicalAddress, pdataPhysicalAddress, pdataSize);

        return baos.toByteArray();
    }

    /**
     * generateBssSegment
     *
     * @brief
     * 功能: 生成.bss段配置结构。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @return .bss段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generateBssSegment(Module module, SharedLibrary sharedLibrary, 
                                           long libRegionBaseAddress, int index, 
                                           Map<String, byte[]> fieldMap, SegmentInfo prevSegmentInfo, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Bss_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x4"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(BSS_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - 前一个段部署地址 + 前一个段大小
        // prevSegmentInfo 可能是 .data 段（如果 .pdata 不存在）或 .pdata 段（如果存在）
        long bssDeployAddress = prevSegmentInfo.deployAddress + prevSegmentInfo.size;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(bssDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - 前一个段逻辑地址 + 前一个段大小
        long bssLogicalAddress = prevSegmentInfo.logicalAddress + prevSegmentInfo.size;
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(bssLogicalAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - 前一个段物理地址 + 前一个段大小
        long bssPhysicalAddress = prevSegmentInfo.physicalAddress + prevSegmentInfo.size;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(bssPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizeBss属性值
        long bssSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeBss());
        byte[] bssSizeBytes = BinaryUtils.longTo4BytesBigEndian(bssSize);
        baos.write(bssSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", bssSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");

        // 保存段信息供后续段使用
        segmentInfoHolder[0] = new SegmentInfo(bssDeployAddress, bssLogicalAddress, bssPhysicalAddress, bssSize);

        return baos.toByteArray();
    }

    /**
     * generatePbssSegment
     *
     * @brief
     * 功能: 生成.pbss段配置结构仅当分区操作系统共享库存在MemorySizePersistentBss属性时才生成。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @param[in]  libRegionBaseAddress: 库区域基地址
     * @param[in]  index: 共享库索引
     * @param[in]  fieldMap: 字段映射
     * @param[in]  dataOrPdataInfo: 部署地址基准段信息（.data或.pdata段）
     * @param[in]  bssInfo: 逻辑/物理地址基准段信息（.bss段）
     * @return .pbss段二进制数据
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] generatePbssSegment(Module module, SharedLibrary sharedLibrary, 
                                             long libRegionBaseAddress, int index, 
                                             Map<String, byte[]> fieldMap, SegmentInfo dataOrPdataInfo, SegmentInfo bssInfo, SegmentInfo[] segmentInfoHolder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "SharedLibrary_" + index + "_Pbss_";

        // 1. 域空间名 (32字节)
        String domainName = sharedLibrary.getfNameAsString();
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(domainName, 32, '\0');
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainName", domainNameBytes);

        // 2. 域类型 (4字节) - 固定值正整数"1"
        byte[] domainTypeBytes = BinaryUtils.intToBytesBigEndian(DOMAIN_TYPE);
        baos.write(domainTypeBytes);
        fieldMap.put(baseFieldName + "DomainType", domainTypeBytes);

        // 3. 占位符 (4字节) - 固定值正整数"0"
        byte[] placeholderBytes = BinaryUtils.intToBytesBigEndian(PLACEHOLDER);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 4. 段类型 (4字节) - 固定值正整数"0x20"
        byte[] segmentTypeBytes = BinaryUtils.intToBytesBigEndian(PBSS_SEGMENT_TYPE);
        baos.write(segmentTypeBytes);
        fieldMap.put(baseFieldName + "SegmentType", segmentTypeBytes);

        // 5. 部署地址 (4字节，32位 unsigned long) - .data或.pdata段部署地址 + 对应段大小
        long pbssDeployAddress = dataOrPdataInfo.deployAddress + dataOrPdataInfo.size;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesBigEndian(pbssDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put(baseFieldName + "DeployAddress", deployAddressBytes);

        // 6. 逻辑地址 (4字节，32位 unsigned long) - .bss段逻辑地址 + .bss段大小
        long pbssLogicalAddress = bssInfo.logicalAddress + bssInfo.size;
        byte[] virtualAddressBytes = BinaryUtils.longTo4BytesBigEndian(pbssLogicalAddress);
        baos.write(virtualAddressBytes);
        fieldMap.put(baseFieldName + "VirtualAddress", virtualAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Before_PhysicalAddress");

        // 7. 物理地址 (8字节) - .bss段物理地址 + .bss段大小
        long pbssPhysicalAddress = bssInfo.physicalAddress + bssInfo.size;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesBigEndian(pbssPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 8. 段大小 (4字节，32位 unsigned long) - MemorySizePersistentBss属性值
        long pbssSize = parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizePersistentBss());
        byte[] pbssSizeBytes = BinaryUtils.longTo4BytesBigEndian(pbssSize);
        baos.write(pbssSizeBytes);
        fieldMap.put(baseFieldName + "SegmentSize", pbssSizeBytes);

        // 9. 校验和 (4字节) - 固定值正整数"0x1234"
        byte[] checksumBytes = BinaryUtils.intToBytesBigEndian(CHECKSUM);
        baos.write(checksumBytes);
        fieldMap.put(baseFieldName + "Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, baseFieldName + "Padding_Final");
        
        // 保存段信息供计算下一个共享库的物理地址
        segmentInfoHolder[0] = new SegmentInfo(pbssDeployAddress, pbssLogicalAddress, pbssPhysicalAddress, pbssSize);

        return baos.toByteArray();
    }

    /**
     * getSharedLibraryBaseAddress
     *
     * @brief
     * 功能: 获取共享库的基地址。
     *
     * @param[in]  module: Module对象
     * @param[in]  sharedLibrary: 共享库对象
     * @return 基地址
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getSharedLibraryBaseAddress(Module module, SharedLibrary sharedLibrary) {
        long nextAddress = module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory().getfBaseAddress()
                + calculateCoreOSSizeWithoutBss(module);

        // 按 Payload 顺序累加前置共享库的非 BSS 段，作为缺省部署地址
        if (module.getfPayload() != null && module.getfPayload().getfSharedLibraryPayloads() != null) {
            for (Payload.SharedLibraryPayload payload : module.getfPayload().getfSharedLibraryPayloads()) {
                if (payload.getfNameRefAsString().equals(sharedLibrary.getfNameAsString())) {
                    long baseAddress = payload.getBaseAddress();
                    return baseAddress != 0 ? baseAddress : nextAddress;
                }

                if (payload.getBaseAddress() != 0) {
                    nextAddress = payload.getBaseAddress();
                }
                nextAddress += calculateSharedLibrarySizeWithoutBss(module, payload.getfNameRefAsString());
            }
        }
        return nextAddress;
    }

    /**
     * calculateSharedLibrarySizeWithoutBss
     *
     * @brief
     * 功能: 执行 calculateSharedLibrarySizeWithoutBss 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static long calculateSharedLibrarySizeWithoutBss(Module module, String sharedLibraryName) {
        SharedLibrary library = module.getfSharedLibraries().get(sharedLibraryName);
        if (library == null || library.getfMemorySize() == null) {
            return 0L;
        }
        SharedLibrary.MemorySize size = library.getfMemorySize();
        return parseHexOrDecLong(size.getfMemorySizeText())
                + parseHexOrDecLong(size.getfMemorySizeRoData())
                + parseHexOrDecLong(size.getfMemorySizeData())
                + parseHexOrDecLong(size.getfMemorySizePersistentData());
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
    private static long calculateCoreOSSizeWithoutBss(Module module) {
        if (module != null && module.getfCoreOS() != null && 
            module.getfCoreOS().getfMemorySize() != null) {
            
            CoreOS.MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long dataSize = memorySize.getfMemorySizeData();
            long roDataSize = memorySize.getfMemorySizeRoData();
            long textSize = memorySize.getfMemorySizeText();
            
            long totalSize = dataSize + roDataSize + textSize;
            return totalSize;
        }
        
        return 0L;
    }
    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算分区操作系统共享库部署信息配置的总大小。
     *
     * @param[in]  module: Module对象
     * @return 总大小
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        if (module.getfSharedLibraries() == null || module.getfSharedLibraries().isEmpty()) {
            return 0;
        }

        int totalSize = 0;
        
        // 每个段的结构大小（32位系统）：
        // 32字节(域空间名) + 4字节(域类型) + 4字节(占位符) + 4字节(段类型) + 
        // 4字节(部署地址) + 4字节(逻辑地址) + 8字节(物理地址) + 4字节(段大小) + 4字节(校验和) = 68字节
        // 8字节对齐后 = 72字节
        final int SINGLE_SEGMENT_SIZE = 72;

        for (Map.Entry<String, SharedLibrary> entry : module.getfSharedLibraries().entrySet()) {
            SharedLibrary sharedLibrary = entry.getValue();
            if (sharedLibrary == null) {
                continue;
            }

            // 每个共享库至少包含4个段：.text、.rodata、.data、.bss
            int segmentsPerLibrary = 4;
            
            // 如果存在MemorySizePersistentData且不为空，则还有.pdata段
            // 修复：移除 > 0 的判断，只要配置了就计数
            if (sharedLibrary.getfMemorySize() != null && 
                sharedLibrary.getfMemorySize().getfMemorySizePersistentData() != null &&
                !sharedLibrary.getfMemorySize().getfMemorySizePersistentData().trim().isEmpty()) {
                segmentsPerLibrary++;
            }
            
            //如果存在MemorySizePersistentBss且不为空，则还有.pbss段
            if (sharedLibrary.getfMemorySize() != null && 
                sharedLibrary.getfMemorySize().getfMemorySizePersistentBss() != null &&
                !sharedLibrary.getfMemorySize().getfMemorySizePersistentBss().trim().isEmpty()) {
                segmentsPerLibrary++;
            }
            
            totalSize += segmentsPerLibrary * SINGLE_SEGMENT_SIZE;
        }

        return totalSize;
    }

    /**
     * parseHexOrDecLong
     *
     * @brief
     * 功能: 解析十六进制或十进制字符串为长整数。
     *
     * @param[in]  value: 待解析的字符串值
     * @return 解析后的长整数值
     * @scope internal
     * @errno:
     * N/A
     */
    private static long parseHexOrDecLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        
        value = value.trim();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            // 十六进制
            return Long.parseLong(value.substring(2), 16);
        } else {
            // 十进制
            return Long.parseLong(value);
        }
    }
    
    /**
     * extractLibRegionBaseAddress
     *
     * @brief
     * 功能: 提取libRegion的Base_Address映射: /Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/libRegion/@Base_Address。
     *
     * @param[in]  module: Module对象
     * @return libRegion基地址
     * @scope internal
     * @errno:
     * N/A
     */
    private static long extractLibRegionBaseAddress(Module module) {
        if (module != null && 
            module.getfCoreOS() != null && 
            module.getfCoreOS().getfHardwareConfiguration() != null &&
            module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            
            String libRegionBaseAddressStr = 
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfLibRegionBaseAddress();
            
            if (libRegionBaseAddressStr != null && !libRegionBaseAddressStr.trim().isEmpty()) {
                long baseAddress = parseHexOrDecLong(libRegionBaseAddressStr);
                return baseAddress;
            }
        }
        
        // 如果未配置，使用默认值
        long defaultAddress = 0x40000000L;
        return defaultAddress;
    }
    
    /**
     * add8ByteAlignmentIfNeeded
     *
     * @brief
     * 功能: 在8字节字段前检查是否需要padding来达到8字节对齐如果需要，添加padding。
     *
     * @param[in]  baos: 字节流
     * @param[in]  fieldMap: 字段映射
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
