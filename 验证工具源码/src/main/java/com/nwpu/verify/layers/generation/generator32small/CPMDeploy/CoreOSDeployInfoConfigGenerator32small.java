/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreOSDeployInfoConfigGenerator32small.java
 * 
 * 描述    : 生成CoreOSDeployInfoConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMDeploy;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Payload;
import com.nwpu.verify.entity.CoreOS;
import com.nwpu.verify.entity.CoreOS.MemorySize;
import com.nwpu.verify.entity.CoreOS.KernelConfiguration;
import com.nwpu.verify.entity.CoreOS.HardwareConfiguration.PhysicalMemory;
import com.nwpu.verify.entity.SharedLibrary;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 核心OS部署信息配置存储结构生成器 (32位大端)。
 */
public class CoreOSDeployInfoConfigGenerator32small {

    // 段类型常量
    private static final int SEGMENT_TYPE_TEXT = 0x1; // .text段
    private static final int SEGMENT_TYPE_RODATA = 0x8; // .rodata段
    private static final int SEGMENT_TYPE_DATA = 0x2; // .data段
    private static final int SEGMENT_TYPE_BSS = 0x4; // .bss段
    private static final int SEGMENT_TYPE_CONFIG_RECORD = 0x8; // .configRecord段

    // 固定值常量
    private static final int DOMAIN_TYPE_FIXED = 0; // 域类型固定值
    private static final int DOMAIN_TYPE_CONFIG_RECORD = 2; // .configRecord段域类型固定值
    private static final int PLACEHOLDER_FIXED = 0; // 占位符固定值
    private static final int CHECKSUM_FIXED = 0x1234; // 校验和固定值

    // 域空间名长度
    private static final int DOMAIN_NAME_LENGTH = 32;

    // 段地址和大小缓存（用于链式计算）
    private long textDeployAddress = 0;
    private long textLogicalAddress = 0;
    private long textPhysicalAddress = 0;
    private long textSegmentSize = 0;

    private long rodataDeployAddress = 0;
    private long rodataLogicalAddress = 0;
    private long rodataPhysicalAddress = 0;
    private long rodataSegmentSize = 0;

    private long dataDeployAddress = 0;
    private long dataLogicalAddress = 0;
    private long dataPhysicalAddress = 0;
    private long dataSegmentSize = 0;

    /**
     * generateBin
     *
     * @brief
     * 功能: 生成核心OS部署信息配置存储结构的二进制内容。
     *
     * @param[in]  module:   从XML解析层传入的顶层Module对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射，供对比层使用
     * @return 生成的完整二进制字节数组
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateBin(Module module, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 按照段的顺序依次生成
            // 1. .text 段
            byte[] textSegmentBytes = generateTextSegment(module, fieldMap);
            baos.write(textSegmentBytes);

            // 2. .rodata 段
            byte[] rodataSegmentBytes = generateRodataSegment(module, fieldMap);
            baos.write(rodataSegmentBytes);

            // 3. .data 段
            byte[] dataSegmentBytes = generateDataSegment(module, fieldMap);
            baos.write(dataSegmentBytes);

            // 4. .bss 段
            byte[] bssSegmentBytes = generateBssSegment(module, fieldMap);
            baos.write(bssSegmentBytes);

            // 5. .configRecord 段
            byte[] configRecordSegmentBytes = generateConfigRecordSegment(module, fieldMap);
            baos.write(configRecordSegmentBytes);

            return baos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * generateTextSegment
     *
     * @brief
     * 功能: 生成核心OS部署信息.text段配置数据结构。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return .text段二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateTextSegment(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 域空间名 (32字节字符数组)
        // 映射: /Module/CoreOS/CoreOSDescription/@KernelName
        String kernelName = extractKernelName(module);
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(kernelName, DOMAIN_NAME_LENGTH, '\0');
        baos.write(domainNameBytes);
        fieldMap.put("CoreOSDeployInfo_Text_DomainName", domainNameBytes);

        // 2. 域类型 (4字节 unsigned int)
        // 固定值: 0
        byte[] domainTypeBytes = BinaryUtils.intToBytesLittleEndian(DOMAIN_TYPE_FIXED);
        baos.write(domainTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Text_DomainType", domainTypeBytes);

        // 3. 占位符 (4字节 unsigned int)
        // 固定值: 0
        byte[] placeholderBytes = BinaryUtils.intToBytesLittleEndian(PLACEHOLDER_FIXED);
        baos.write(placeholderBytes);
        fieldMap.put("CoreOSDeployInfo_Text_Placeholder", placeholderBytes);

        // 4. 部署段信息
        byte[] segmentInfoBytes = generateTextSegmentInfo(module, fieldMap);
        baos.write(segmentInfoBytes);

        return baos.toByteArray();
    }

    /**
     * generateTextSegmentInfo
     *
     * @brief
     * 功能: 生成.text段的部署段信息。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return 部署段信息二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateTextSegmentInfo(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 段类型 (4字节 unsigned int)
        // 固定值: 0x1 (.text段)
        byte[] segmentTypeBytes = BinaryUtils.intToBytesLittleEndian(SEGMENT_TYPE_TEXT);
        baos.write(segmentTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Text_SegmentType", segmentTypeBytes);

        // 2. 部署地址 (4字节，32位 unsigned long)
        // 映射: /Module/Payloads/CoreOSPayload/@Base_Address
        textDeployAddress = extractDeployAddress(module);
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesLittleEndian(textDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Text_DeployAddress", deployAddressBytes);

        // 3. 逻辑地址 (4字节，32位 unsigned long)
        // 映射:
        // /Module/CoreOS/CoreOSDescription/KernelConfiguration/@kernelVirtualAddress
        textLogicalAddress = extractLogicalAddress(module);
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesLittleEndian(textLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Text_LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐（段类型4 + 部署地址4 + 逻辑地址4 = 12字节，需要4字节padding）
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Text_Padding_Before_PhysicalAddress");

        // 4. 物理地址 (8字节 unsigned long)
        // 映射:
        // /Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/@Base_Address
        textPhysicalAddress = extractPhysicalAddress(module);
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesLittleEndian(textPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Text_PhysicalAddress", physicalAddressBytes);

        // 5. 段大小 (4字节，32位 unsigned long)
        // 映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeText
        textSegmentSize = extractTextSegmentSize(module);
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesLittleEndian(textSegmentSize);
        baos.write(segmentSizeBytes);
        fieldMap.put("CoreOSDeployInfo_Text_SegmentSize", segmentSizeBytes);

        // 6. 校验和 (4字节 unsigned int)
        // 固定值: 0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesLittleEndian(CHECKSUM_FIXED);
        baos.write(checksumBytes);
        fieldMap.put("CoreOSDeployInfo_Text_Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Text_Padding_Final");

        return baos.toByteArray();
    }

    /**
     * generateRodataSegment
     *
     * @brief
     * 功能: 生成核心OS部署信息.rodata段配置数据结构。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return .rodata段二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateRodataSegment(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 域空间名 (32字节字符数组)
        // 映射: /Module/CoreOS/CoreOSDescription/@KernelName
        String kernelName = extractKernelName(module);
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(kernelName, DOMAIN_NAME_LENGTH, '\0');
        baos.write(domainNameBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_DomainName", domainNameBytes);

        // 2. 域类型 (4字节 unsigned int)
        // 固定值: 0
        byte[] domainTypeBytes = BinaryUtils.intToBytesLittleEndian(DOMAIN_TYPE_FIXED);
        baos.write(domainTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_DomainType", domainTypeBytes);

        // 3. 占位符 (4字节 unsigned int)
        // 固定值: 0
        byte[] placeholderBytes = BinaryUtils.intToBytesLittleEndian(PLACEHOLDER_FIXED);
        baos.write(placeholderBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_Placeholder", placeholderBytes);

        // 4. 部署段信息
        byte[] segmentInfoBytes = generateRodataSegmentInfo(module, fieldMap);
        baos.write(segmentInfoBytes);

        return baos.toByteArray();
    }

    /**
     * generateRodataSegmentInfo
     *
     * @brief
     * 功能: 生成.rodata段的部署段信息。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return 部署段信息二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateRodataSegmentInfo(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 段类型 (4字节 unsigned int)
        // 固定值: 0x8 (.rodata段)
        byte[] segmentTypeBytes = BinaryUtils.intToBytesLittleEndian(SEGMENT_TYPE_RODATA);
        baos.write(segmentTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_SegmentType", segmentTypeBytes);

        // 2. 部署地址 (4字节，32位 unsigned long)
        // 计算方式: .text段部署地址 + .text段大小
        rodataDeployAddress = textDeployAddress + textSegmentSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesLittleEndian(rodataDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_DeployAddress", deployAddressBytes);

        // 3. 逻辑地址 (4字节，32位 unsigned long)
        // 计算方式: .text段逻辑地址 + .text段大小
        rodataLogicalAddress = textLogicalAddress + textSegmentSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesLittleEndian(rodataLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Rodata_Padding_Before_PhysicalAddress");

        // 4. 物理地址 (8字节 unsigned long long)
        // 计算方式: .text段物理地址 + .text段大小
        rodataPhysicalAddress = textPhysicalAddress + textSegmentSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesLittleEndian(rodataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_PhysicalAddress", physicalAddressBytes);

        // 5. 段大小 (4字节，32位 unsigned long)
        // 映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeRoData
        rodataSegmentSize = extractRodataSegmentSize(module);
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesLittleEndian(rodataSegmentSize);
        baos.write(segmentSizeBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_SegmentSize", segmentSizeBytes);

        // 6. 校验和 (4字节 unsigned int)
        // 固定值: 0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesLittleEndian(CHECKSUM_FIXED);
        baos.write(checksumBytes);
        fieldMap.put("CoreOSDeployInfo_Rodata_Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Rodata_Padding_Final");

        return baos.toByteArray();
    }

    /**
     * generateDataSegment
     *
     * @brief
     * 功能: 生成核心OS部署信息.data段配置数据结构。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return .data段二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateDataSegment(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 域空间名 (32字节字符数组)
        // 映射: /Module/CoreOS/CoreOSDescription/@KernelName
        String kernelName = extractKernelName(module);
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(kernelName, DOMAIN_NAME_LENGTH, '\0');
        baos.write(domainNameBytes);
        fieldMap.put("CoreOSDeployInfo_Data_DomainName", domainNameBytes);

        // 2. 域类型 (4字节 unsigned int)
        // 固定值: 0
        byte[] domainTypeBytes = BinaryUtils.intToBytesLittleEndian(DOMAIN_TYPE_FIXED);
        baos.write(domainTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Data_DomainType", domainTypeBytes);

        // 3. 占位符 (4字节 unsigned int)
        // 固定值: 0
        byte[] placeholderBytes = BinaryUtils.intToBytesLittleEndian(PLACEHOLDER_FIXED);
        baos.write(placeholderBytes);
        fieldMap.put("CoreOSDeployInfo_Data_Placeholder", placeholderBytes);

        // 4. 部署段信息
        byte[] segmentInfoBytes = generateDataSegmentInfo(module, fieldMap);
        baos.write(segmentInfoBytes);

        return baos.toByteArray();
    }

    /**
     * generateDataSegmentInfo
     *
     * @brief
     * 功能: 生成.data段的部署段信息。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return 部署段信息二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateDataSegmentInfo(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 段类型 (4字节 unsigned int)
        // 固定值: 0x2 (.data段)
        byte[] segmentTypeBytes = BinaryUtils.intToBytesLittleEndian(SEGMENT_TYPE_DATA);
        baos.write(segmentTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Data_SegmentType", segmentTypeBytes);

        // 2. 部署地址 (4字节，32位 unsigned long)
        // 计算方式: .rodata段部署地址 + .rodata段大小
        dataDeployAddress = rodataDeployAddress + rodataSegmentSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesLittleEndian(dataDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Data_DeployAddress", deployAddressBytes);

        // 3. 逻辑地址 (4字节，32位 unsigned long)
        // 计算方式: .rodata段逻辑地址 + .rodata段大小
        dataLogicalAddress = rodataLogicalAddress + rodataSegmentSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesLittleEndian(dataLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Data_LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Data_Padding_Before_PhysicalAddress");

        // 4. 物理地址 (8字节 unsigned long long)
        // 计算方式: .rodata段物理地址 + .rodata段大小
        dataPhysicalAddress = rodataPhysicalAddress + rodataSegmentSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesLittleEndian(dataPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Data_PhysicalAddress", physicalAddressBytes);

        // 5. 段大小 (4字节，32位 unsigned long)
        // 映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeData
        dataSegmentSize = extractDataSegmentSize(module);
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesLittleEndian(dataSegmentSize);
        baos.write(segmentSizeBytes);
        fieldMap.put("CoreOSDeployInfo_Data_SegmentSize", segmentSizeBytes);

        // 6. 校验和 (4字节 unsigned int)
        // 固定值: 0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesLittleEndian(CHECKSUM_FIXED);
        baos.write(checksumBytes);
        fieldMap.put("CoreOSDeployInfo_Data_Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Data_Padding_Final");

        return baos.toByteArray();
    }

    /**
     * generateBssSegment
     *
     * @brief
     * 功能: 生成核心OS部署信息.bss段配置数据结构。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return .bss段二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateBssSegment(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 域空间名 (32字节字符数组)
        // 映射: /Module/CoreOS/CoreOSDescription/@KernelName
        String kernelName = extractKernelName(module);
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(kernelName, DOMAIN_NAME_LENGTH, '\0');
        baos.write(domainNameBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_DomainName", domainNameBytes);

        // 2. 域类型 (4字节 unsigned int)
        // 固定值: 0
        byte[] domainTypeBytes = BinaryUtils.intToBytesLittleEndian(DOMAIN_TYPE_FIXED);
        baos.write(domainTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_DomainType", domainTypeBytes);

        // 3. 占位符 (4字节 unsigned int)
        // 固定值: 0
        byte[] placeholderBytes = BinaryUtils.intToBytesLittleEndian(PLACEHOLDER_FIXED);
        baos.write(placeholderBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_Placeholder", placeholderBytes);

        // 4. 部署段信息
        byte[] segmentInfoBytes = generateBssSegmentInfo(module, fieldMap);
        baos.write(segmentInfoBytes);

        return baos.toByteArray();
    }

    /**
     * generateBssSegmentInfo
     *
     * @brief
     * 功能: 生成.bss段的部署段信息。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return 部署段信息二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateBssSegmentInfo(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 段类型 (4字节 unsigned int)
        // 固定值: 0x4 (.bss段)
        byte[] segmentTypeBytes = BinaryUtils.intToBytesLittleEndian(SEGMENT_TYPE_BSS);
        baos.write(segmentTypeBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_SegmentType", segmentTypeBytes);

        // 2. 部署地址 (4字节，32位 unsigned long)
        // 计算方式: .data段部署地址 + .data段大小
        long bssDeployAddress = dataDeployAddress + dataSegmentSize;
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesLittleEndian(bssDeployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_DeployAddress", deployAddressBytes);

        // 3. 逻辑地址 (4字节，32位 unsigned long)
        // 计算方式: .data段逻辑地址 + .data段大小
        long bssLogicalAddress = dataLogicalAddress + dataSegmentSize;
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesLittleEndian(bssLogicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Bss_Padding_Before_PhysicalAddress");

        // 4. 物理地址 (8字节 unsigned long long)
        // 计算方式: .data段物理地址 + .data段大小
        long bssPhysicalAddress = dataPhysicalAddress + dataSegmentSize;
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesLittleEndian(bssPhysicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_PhysicalAddress", physicalAddressBytes);

        // 5. 段大小 (4字节，32位 unsigned long)
        // 映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeBss
        long bssSegmentSize = extractBssSegmentSize(module);
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesLittleEndian(bssSegmentSize);
        baos.write(segmentSizeBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_SegmentSize", segmentSizeBytes);

        // 6. 校验和 (4字节 unsigned int)
        // 固定值: 0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesLittleEndian(CHECKSUM_FIXED);
        baos.write(checksumBytes);
        fieldMap.put("CoreOSDeployInfo_Bss_Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_Bss_Padding_Final");

        return baos.toByteArray();
    }

    /**
     * generateConfigRecordSegment
     *
     * @brief
     * 功能: 生成核心OS部署信息.configRecord段配置数据结构。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return .configRecord段二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateConfigRecordSegment(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 域空间名 (32字节字符数组)
        // 映射: /Module/CoreOS/CoreOSDescription/@KernelName
        String kernelName = extractKernelName(module);
        byte[] domainNameBytes = BinaryUtils.fixedLengthAscii(kernelName, DOMAIN_NAME_LENGTH, '\0');
        baos.write(domainNameBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_DomainName", domainNameBytes);

        // 2. 域类型 (4字节 unsigned int)
        // 固定值: 2
        byte[] domainTypeBytes = BinaryUtils.intToBytesLittleEndian(DOMAIN_TYPE_CONFIG_RECORD);
        baos.write(domainTypeBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_DomainType", domainTypeBytes);

        // 3. 占位符 (4字节 unsigned int)
        // 固定值: 0
        byte[] placeholderBytes = BinaryUtils.intToBytesLittleEndian(PLACEHOLDER_FIXED);
        baos.write(placeholderBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_Placeholder", placeholderBytes);

        // 4. 部署段信息
        byte[] segmentInfoBytes = generateConfigRecordSegmentInfo(module, fieldMap);
        baos.write(segmentInfoBytes);

        return baos.toByteArray();
    }

    /**
     * generateConfigRecordSegmentInfo
     *
     * @brief
     * 功能: 生成.configRecord段的部署段信息。
     *
     * @param[in]  module:   模块对象
     * @param[in]  fieldMap: 字段映射
     * @return 部署段信息二进制数据
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateConfigRecordSegmentInfo(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. 段类型 (4字节 unsigned int)
        // 固定值: 0x8 (.configRecord段)
        byte[] segmentTypeBytes = BinaryUtils.intToBytesLittleEndian(SEGMENT_TYPE_CONFIG_RECORD);
        baos.write(segmentTypeBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_SegmentType", segmentTypeBytes);

        // 2. 部署地址 (4字节，32位 unsigned long)
        // 映射: /Module/Payloads/ConfigRecordPayload/@Base_Address
        // 如果不存在，按照 module/payloads 下的顺序依次分配
        long deployAddress = extractConfigRecordDeployAddress(module);
        byte[] deployAddressBytes = BinaryUtils.longTo4BytesLittleEndian(deployAddress);
        baos.write(deployAddressBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_DeployAddress", deployAddressBytes);

        // 3. 逻辑地址 (4字节，32位 unsigned long)
        // 映射: /Module/CoreOS/CoreOSDescription/KernelConfiguration/@kernelVirtualAddress
        //       + /Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/kernelMemoryRegion/@Size
        long logicalAddress = extractConfigRecordLogicalAddress(module);
        byte[] logicalAddressBytes = BinaryUtils.longTo4BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_LogicalAddress", logicalAddressBytes);

        // 在8字节物理地址字段前进行对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_ConfigRecord_Padding_Before_PhysicalAddress");

        // 4. 物理地址 (8字节 unsigned long long)
        // 映射: PhysicalMemory/@Base_Address + kernelMemoryRegion/@Size
        long physicalAddress = extractConfigRecordPhysicalAddress(module);
        byte[] physicalAddressBytes = BinaryUtils.longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_PhysicalAddress", physicalAddressBytes);

        // 5. 段大小 (4字节，32位 unsigned long)
        // 映射:
        // /Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/kernelConfigRecordRegion/@Size
        long segmentSize = extractConfigRecordSegmentSize(module);
        byte[] segmentSizeBytes = BinaryUtils.longTo4BytesLittleEndian(segmentSize);
        baos.write(segmentSizeBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_SegmentSize", segmentSizeBytes);

        // 6. 校验和 (4字节 unsigned int)
        // 固定值: 0x1234
        byte[] checksumBytes = BinaryUtils.intToBytesLittleEndian(CHECKSUM_FIXED);
        baos.write(checksumBytes);
        fieldMap.put("CoreOSDeployInfo_ConfigRecord_Checksum", checksumBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "CoreOSDeployInfo_ConfigRecord_Padding_Final");

        return baos.toByteArray();
    }

    /**
     * extractKernelName
     *
     * @brief
     * 功能: 提取内核名称映射: /Module/CoreOS/CoreOSDescription/@KernelName。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private String extractKernelName(Module module) {
        if (module != null && module.getfCoreOS() != null) {
            String kernelName = module.getfCoreOS().getKernelNameAsString();
            if (kernelName != null && !kernelName.trim().isEmpty()) {
                return kernelName;
            }
        }
        System.out.println("Warning: KernelName not found, using default value");
        return "DefaultKernel"; // 默认值
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
    private long extractDeployAddress(Module module) {
        if (module == null || module.getfPayload() == null) {
            long defaultAddress = 0x08000000L;
            System.out.println("Warning: Payload is null, using default deploy address 0x" + Long.toHexString(defaultAddress).toUpperCase());
            return defaultAddress;
        }

        Payload.CoreOSPayload coreOSPayload = module.getfPayload().getfCoreOSPayload();

        // 1. 尝试从CoreOSPayload中获取Base_Address
        if (coreOSPayload != null) {
            long baseAddress = coreOSPayload.getBaseAddress();
            if (baseAddress != 0) {
                System.out.println("Read CoreOS deploy address from XML: 0x" + Long.toHexString(baseAddress).toUpperCase());
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
                System.out.println("Auto-allocated CoreOS deploy address from payloadMemory: 0x" +
                        Long.toHexString(payloadBaseAddress).toUpperCase());
                return payloadBaseAddress;
            }
        }

        // 3. 如果都不存在，使用默认值
        long defaultAddress = 0x08000000L;
        System.out.println("Warning: CoreOSPayload Base_Address not configured and payloadMemory does not exist, using default deploy address 0x" +
                Long.toHexString(defaultAddress).toUpperCase());
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
    private long extractConfigRecordDeployAddress(Module module) {
        if (module == null || module.getfPayload() == null) {
            long defaultAddress = 0x08000000L;
            System.out.println("Warning: Payload is null, using default ConfigRecord deploy address 0x" +
                    Long.toHexString(defaultAddress).toUpperCase());
            return defaultAddress;
        }

        Payload payload = module.getfPayload();

        // 1. 尝试从 ConfigRecordPayload 列表中获取第一个的 Base_Address
        // 注意：这里假设 configRecord 段对应的是第一个 ConfigRecordPayload
        if (payload.getfConfigRecordPayloads() != null && !payload.getfConfigRecordPayloads().isEmpty()) {
            Payload.ConfigRecordPayload configRecordPayload = payload.getfConfigRecordPayloads().get(0);
            long baseAddress = configRecordPayload.getBaseAddress();
            if (baseAddress != 0) {
                System.out.println("Read ConfigRecord deploy address from XML: 0x" + Long.toHexString(baseAddress).toUpperCase());
                return baseAddress;
            }
        }

        // 2. 如果 Base_Address 不存在或为0，只关注前一个 SharedLibraryPayload（最后一个共享库）
        if (payload.getfSharedLibraryPayloads() != null && !payload.getfSharedLibraryPayloads().isEmpty()) {
            Payload.SharedLibraryPayload previousSharedLibPayload = payload.getfSharedLibraryPayloads()
                    .get(payload.getfSharedLibraryPayloads().size() - 1);

            long previousBaseAddress = previousSharedLibPayload.getBaseAddress();
            String previousName = previousSharedLibPayload.getfNameRefAsString();

            if (previousBaseAddress != 0) {
                long previousSharedLibSize = calculateSharedLibrarySizeWithoutBss(module, previousName);
                long configRecordAddress = previousBaseAddress + previousSharedLibSize;

                System.out.println("Computed ConfigRecord deploy address from previous SharedLibraryPayload(" + previousName + "): " +
                        "0x" + Long.toHexString(previousBaseAddress).toUpperCase() +
                        " + 0x" + Long.toHexString(previousSharedLibSize).toUpperCase() +
                        " = 0x" + Long.toHexString(configRecordAddress).toUpperCase());
                return configRecordAddress;
            }

            System.out.println("Warning: previous SharedLibraryPayload(" + previousName + ") Base_Address not configured, falling back to legacy auto-allocation logic");
        }

        // 3. 回退：如果不存在可用前置SharedLibraryPayload，沿用旧自动分配逻辑
        // 从 payloadMemory 的 BaseAddress 开始累加各个 payload 的大小
        long currentAddress = 0L;

        // 获取 payloadMemory 的起始地址
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPayloadMemory() != null) {

            CoreOS.HardwareConfiguration.PayloadMemory payloadMemory = module.getfCoreOS().getfHardwareConfiguration()
                    .getfPayloadMemory();
            currentAddress = payloadMemory.getfBaseAddress();

            System.out.println("Auto-allocated ConfigRecord address from payloadMemory base address: 0x" +
                    Long.toHexString(currentAddress).toUpperCase());
        } else {
            currentAddress = 0x08000000L; // 默认起始地址
            System.out.println("Warning: payloadMemory does not exist, using default base address 0x" +
                    Long.toHexString(currentAddress).toUpperCase());
        }

        // 累加 CoreOSPayload 的大小 (不包含BSS)
        if (payload.getfCoreOSPayload() != null) {
            long coreOSSize = calculateCoreOSSizeWithoutBss(module);
            System.out.println("  + CoreOSPayload size (without BSS): 0x" + Long.toHexString(coreOSSize).toUpperCase());
            currentAddress += coreOSSize;
        }

        // 累加所有 SharedLibraryPayload 的大小 (不包含BSS)
        if (payload.getfSharedLibraryPayloads() != null) {
            for (Payload.SharedLibraryPayload sharedLibPayload : payload.getfSharedLibraryPayloads()) {
                long sharedLibSize = calculateSharedLibrarySizeWithoutBss(module,
                        sharedLibPayload.getfNameRefAsString());
                System.out.println("  + SharedLibraryPayload(" + sharedLibPayload.getfNameRefAsString() +
                        ") size (without BSS): 0x" + Long.toHexString(sharedLibSize).toUpperCase());
                currentAddress += sharedLibSize;
            }
        }

        System.out.println("Auto-allocated ConfigRecord deploy address: 0x" + Long.toHexString(currentAddress).toUpperCase());
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
    private long calculateCoreOSSizeWithoutBss(Module module) {
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
     * calculateSharedLibrarySizeWithoutBss
     *
     * @brief
     * 功能: 计算SharedLibrary的大小，不包含BSS段计算公式: MemorySizeData + MemorySizeRoData + MemorySizeText +MemorySizePersistentData。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculateSharedLibrarySizeWithoutBss(Module module, String sharedLibName) {
        if (module != null && module.getfSharedLibraries() != null) {
            SharedLibrary sharedLib = module.getfSharedLibraries().get(sharedLibName);
            if (sharedLib != null && sharedLib.getfMemorySize() != null) {
                SharedLibrary.MemorySize memorySize = sharedLib.getfMemorySize();

                long dataSize = parseHexString(memorySize.getfMemorySizeData());
                long roDataSize = parseHexString(memorySize.getfMemorySizeRoData());
                long textSize = parseHexString(memorySize.getfMemorySizeText());
                long persistentDataSize = parseHexString(memorySize.getfMemorySizePersistentData());

                long totalSize = dataSize + roDataSize + textSize + persistentDataSize;
                System.out
                        .println("    " + sharedLibName + " section size calculation: Data(0x" + Long.toHexString(dataSize).toUpperCase() +
                                ") + RoData(0x" + Long.toHexString(roDataSize).toUpperCase() +
                                ") + Text(0x" + Long.toHexString(textSize).toUpperCase() +
                                ") + PersistentData(0x" + Long.toHexString(persistentDataSize).toUpperCase() +
                                ") = 0x" + Long.toHexString(totalSize).toUpperCase());
                return totalSize;
            }
        }

        System.out.println("    Warning: SharedLibrary(" + sharedLibName + ") MemorySize does not exist, returning 0");
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
            System.out.println("    Warning: cannot parse hex string '" + hexStr + "', returning 0");
            return 0L;
        }
    }

    /**
     * extractLogicalAddress
     *
     * @brief
     * 功能: 提取逻辑地址映射:/Module/CoreOS/CoreOSDescription/KernelConfiguration/@kernelVirtualAddress。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractLogicalAddress(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null) {

            KernelConfiguration kernelConfig = module.getfCoreOS().getfKernelConfiguration();
            long virtualAddress = kernelConfig.getfKernelVirtualAddress();
            System.out.println("Read logical address from XML: 0x" + Long.toHexString(virtualAddress).toUpperCase());
            return virtualAddress;
        }

        long defaultAddress = 0xC0000000L; // 默认逻辑地址
        System.out.println("Warning: using default logical address 0x" + Long.toHexString(defaultAddress).toUpperCase());
        return defaultAddress;
    }

    /**
     * extractPhysicalAddress
     *
     * @brief
     * 功能: 提取物理地址映射:/Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/@Base_Address。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractPhysicalAddress(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            PhysicalMemory physicalMemory = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory();
            long baseAddress = physicalMemory.getfBaseAddress();
            System.out.println("Read physical address from XML: 0x" + Long.toHexString(baseAddress).toUpperCase());
            return baseAddress;
        }

        long defaultAddress = 0x40000000L; // 默认物理地址
        System.out.println("Warning: using default physical address 0x" + Long.toHexString(defaultAddress).toUpperCase());
        return defaultAddress;
    }

    /**
     * extractTextSegmentSize
     *
     * @brief
     * 功能: 提取.text段大小映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeText。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractTextSegmentSize(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfMemorySize() != null) {

            MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long textSize = memorySize.getfMemorySizeText();
            System.out.println("Read .text section size from XML: " + textSize + " bytes");
            return textSize;
        }

        long defaultSize = 0x10000L; // 默认64KB
        System.out.println("Warning: using default .text section size " + defaultSize + " bytes");
        return defaultSize;
    }

    /**
     * extractRodataSegmentSize
     *
     * @brief
     * 功能: 提取.rodata段大小映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeRoData。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractRodataSegmentSize(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfMemorySize() != null) {

            MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long rodataSize = memorySize.getfMemorySizeRoData();
            System.out.println("Read .rodata section size from XML: " + rodataSize + " bytes");
            return rodataSize;
        }

        long defaultSize = 0x8000L; // 默认32KB
        System.out.println("Warning: using default .rodata section size " + defaultSize + " bytes");
        return defaultSize;
    }

    /**
     * extractDataSegmentSize
     *
     * @brief
     * 功能: 提取.data段大小映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeData。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractDataSegmentSize(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfMemorySize() != null) {

            MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long dataSize = memorySize.getfMemorySizeData();
            System.out.println("Read .data section size from XML: " + dataSize + " bytes");
            return dataSize;
        }

        long defaultSize = 0x4000L; // 默认16KB
        System.out.println("Warning: using default .data section size " + defaultSize + " bytes");
        return defaultSize;
    }

    /**
     * extractBssSegmentSize
     *
     * @brief
     * 功能: 提取.bss段大小映射: /Module/CoreOS/CoreOSDescription/MemorySize/@MemorySizeBss。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractBssSegmentSize(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfMemorySize() != null) {

            MemorySize memorySize = module.getfCoreOS().getfMemorySize();
            long bssSize = memorySize.getfMemorySizeBss();
            System.out.println("Read .bss section size from XML: " + bssSize + " bytes");
            return bssSize;
        }

        long defaultSize = 0x2000L; // 默认8KB
        System.out.println("Warning: using default .bss section size " + defaultSize + " bytes");
        return defaultSize;
    }

    /**
     * extractConfigRecordLogicalAddress
     *
     * @brief
     * 功能: 提取.configRecord段的逻辑地址映射: KernelConfiguration/@kernelVirtualAddress + PhysicalMemory/kernelMemoryRegion/@Size。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractConfigRecordLogicalAddress(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfKernelConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            long kernelVirtualAddress = module.getfCoreOS().getfKernelConfiguration().getfKernelVirtualAddress();
            long kernelMemoryRegionSize = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfKernelMemoryRegionSize();
            long logicalAddress = kernelVirtualAddress + kernelMemoryRegionSize;

            System.out.println("Computed .configRecord logical address from XML: 0x" + Long.toHexString(kernelVirtualAddress).toUpperCase() +
                    " + 0x" + Long.toHexString(kernelMemoryRegionSize).toUpperCase() +
                    " = 0x" + Long.toHexString(logicalAddress).toUpperCase());
            return logicalAddress;
        }

        long defaultAddress = 0x40100000L; // 默认物理地址 + 1MB
        System.out.println("Warning: using default .configRecord logical address 0x" + Long.toHexString(defaultAddress).toUpperCase());
        return defaultAddress;
    }

    /**
     * extractConfigRecordPhysicalAddress
     *
     * @brief
     * 功能: 提取.configRecord段的物理地址映射: PhysicalMemory/@Base_Address + kernelMemoryRegion/@Size。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractConfigRecordPhysicalAddress(Module module) {
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            PhysicalMemory physicalMemory = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory();
            return physicalMemory.getfBaseAddress() + physicalMemory.getfKernelMemoryRegionSize();
        }
        return 0x40100000L;
    }

    /**
     * extractConfigRecordSegmentSize
     *
     * @brief
     * 功能: 提取.configRecord段大小映射:/Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/kernelConfigRecordRegion/@Size。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private long extractConfigRecordSegmentSize(Module module) {
        if (module != null && module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            PhysicalMemory physicalMemory = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory();
            long configRecordSize = 0L;
            String sizeStr = physicalMemory.getfKernelConfigRecordRegionSize();
            try {
                if (sizeStr != null && !sizeStr.trim().isEmpty()) {
                    // 支持十六进制和十进制格式
                    if (sizeStr.startsWith("0x") || sizeStr.startsWith("0X")) {
                        // 十六进制格式：0x100000
                        String hexStr = sizeStr.substring(2);
                        configRecordSize = Long.parseLong(hexStr, 16);
                    } else {
                        // 十进制格式
                        configRecordSize = Long.parseLong(sizeStr);
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("Warning: .configRecord section size parse failed, input value: " + sizeStr + ", using default value");
                configRecordSize = 0L; // 默认0KB
            }
            System.out.println("Read .configRecord section size from XML: " + configRecordSize + " bytes");
            return configRecordSize;
        }

        long defaultSize = 0L;
        System.out.println("Warning: using default .configRecord section size " + defaultSize + " bytes");
        return defaultSize;
    }

    /**
     * calculateTotalSize
     *
     * @brief
     * 功能: 计算核心OS部署信息配置存储结构的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 总大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateTotalSize(Module module) {
        // 计算已实现的段的大小
        int textSegmentSize = calculateTextSegmentSize();
        int rodataSegmentSize = calculateRodataSegmentSize();
        int dataSegmentSize = calculateDataSegmentSize();
        int bssSegmentSize = calculateBssSegmentSize();
        int configRecordSegmentSize = calculateConfigRecordSegmentSize();

        return textSegmentSize + rodataSegmentSize + dataSegmentSize + bssSegmentSize + configRecordSegmentSize;
    }

    /**
     * calculateTextSegmentSize
     *
     * @brief
     * 功能: 计算.text段配置数据结构的大小。
     *
     * @return .text段配置数据结构大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateTextSegmentSize() {
        int size = 0;
        size += DOMAIN_NAME_LENGTH; // 域空间名: 32字节
        size += 4; // 域类型: 4字节
        // size += 4; // 占位符: 4字节
        size += 4; // 段类型: 4字节
        size += 8; // 部署地址: 8字节 (32位系统)
        size += 8; // 逻辑地址: 8字节 (32位系统)
        size += 8; // 物理地址: 8字节
        size += 8; // 段大小: 8字节 (32位系统)
        size += 4; // 校验和: 4字节
        return size; // 总计: 32 + 4*3 + 8*4 = 76字节
    }

    /**
     * calculateRodataSegmentSize
     *
     * @brief
     * 功能: 计算.rodata段配置数据结构的大小。
     *
     * @return .rodata段配置数据结构大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateRodataSegmentSize() {
        int size = 0;
        size += DOMAIN_NAME_LENGTH; // 域空间名: 32字节
        size += 4; // 域类型: 4字节
        // size += 4; // 占位符: 4字节
        size += 4; // 段类型: 4字节
        size += 8; // 部署地址: 8字节 (32位系统)
        size += 8; // 逻辑地址: 8字节 (32位系统)
        size += 8; // 物理地址: 8字节
        size += 8; // 段大小: 8字节 (32位系统)
        size += 4; // 校验和: 4字节
        return size; // 总计: 32 + 4*3 + 8*4 = 76字节（与.text段相同）
    }

    /**
     * calculateDataSegmentSize
     *
     * @brief
     * 功能: 计算.data段配置数据结构的大小。
     *
     * @return .data段配置数据结构大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateDataSegmentSize() {
        int size = 0;
        size += DOMAIN_NAME_LENGTH; // 域空间名: 32字节
        size += 4; // 域类型: 4字节
        // size += 4; // 占位符: 4字节
        size += 4; // 段类型: 4字节
        size += 8; // 部署地址: 8字节 (32位系统)
        size += 8; // 逻辑地址: 8字节 (32位系统)
        size += 8; // 物理地址: 8字节
        size += 8; // 段大小: 8字节 (32位系统)
        size += 4; // 校验和: 4字节
        return size; // 总计: 32 + 4*3 + 8*4 = 76字节（与其他段相同）
    }

    /**
     * calculateBssSegmentSize
     *
     * @brief
     * 功能: 计算.bss段配置数据结构的大小。
     *
     * @return .bss段配置数据结构大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateBssSegmentSize() {
        int size = 0;
        size += DOMAIN_NAME_LENGTH; // 域空间名: 32字节
        size += 4; // 域类型: 4字节
        // size += 4; // 占位符: 4字节
        size += 4; // 段类型: 4字节
        size += 8; // 部署地址: 8字节 (32位系统)
        size += 8; // 逻辑地址: 8字节 (32位系统)
        size += 8; // 物理地址: 8字节
        size += 8; // 段大小: 8字节 (32位系统)
        size += 4; // 校验和: 4字节
        return size; // 总计: 32 + 4*3 + 8*4 = 76字节（与其他段相同）
    }

    /**
     * calculateConfigRecordSegmentSize
     *
     * @brief
     * 功能: 计算.configRecord段配置数据结构的大小。
     *
     * @return .configRecord段配置数据结构大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateConfigRecordSegmentSize() {
        int size = 0;
        size += DOMAIN_NAME_LENGTH; // 域空间名: 32字节
        size += 4; // 域类型: 4字节
        // size += 4; // 占位符: 4字节
        size += 4; // 段类型: 4字节
        size += 8; // 部署地址: 8字节 (32位系统)
        size += 8; // 逻辑地址: 8字节 (32位系统)
        size += 8; // 物理地址: 8字节
        size += 8; // 段大小: 8字节 (32位系统)
        size += 4; // 校验和: 4字节
        return size; // 总计: 32 + 4*3 + 8*4 = 76字节（与其他段相同）
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
    private void add8ByteAlignmentIfNeeded(ByteArrayOutputStream baos,
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
