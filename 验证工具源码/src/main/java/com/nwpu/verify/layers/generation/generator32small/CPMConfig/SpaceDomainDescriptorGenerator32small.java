/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SpaceDomainDescriptorGenerator32small.java
 * 
 * 描述    : 生成SpaceDomainDescriptor相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.entity.SharedLibrary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @brief
 * 功能: 空间域描述信息存储结构生成器 (32位大端)。
 */
public class SpaceDomainDescriptorGenerator32small {

    // =======================================================================
    // 辅助方法
    // =======================================================================

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
     * toNullTerminatedAscii
     *
     * @brief
     * 功能: 将字符串转换为以'\0'结尾的字节数组。。
     *
     * @param[in]  str: 输入字符串
     * @return 字节数组，长度为 str.length() + 1
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] toNullTerminatedAscii(String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.US_ASCII);
        byte[] terminatedBytes = new byte[strBytes.length + 1];
        System.arraycopy(strBytes, 0, terminatedBytes, 0, strBytes.length);
        terminatedBytes[strBytes.length] = 0; // Null terminator
        return terminatedBytes;
    }

    /**
     * addAlignmentPadding
     *
     * @brief
     * 功能: 添加8字节对齐填充。
     *
     * @param[in]  baos: 输出流
     * @param[in]  fieldMap: 字段映射
     * @param[in]  fieldPrefix: 字段前缀
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private void addAlignmentPadding(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap, String fieldPrefix) throws IOException {
        int currentSize = baos.size();
        int alignedSize = alignTo8Bytes(currentSize);
        int paddingSize = alignedSize - currentSize;
        
        if (paddingSize > 0) {
            byte[] paddingBytes = new byte[paddingSize];
            baos.write(paddingBytes);
            fieldMap.put(fieldPrefix + "AlignmentPadding", paddingBytes);
        }
    }

    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 计算8字节对齐的大小。
     *
     * @param[in]  size: 当前大小
     * @return 对齐后的大小
     * @scope internal
     * @errno:
     * N/A
     */
    private int alignTo8Bytes(int size) {
        return (size + 7) & ~7;
    }

    // =======================================================================
    // 主控生成方法
    // =======================================================================

    /**
     * generateAllSpaceDomainDescriptors
     *
     * @brief
     * 功能: 按顺序生成所有空间域描述信息结构的二进制内容。。
     *
     * @param[in]  module: 包含所有配置信息的Module对象。
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射。
     * @return 生成的完整二进制字节数组。
     * @throws IOException 写入流时可能发生异常。
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateAllSpaceDomainDescriptors(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 顺序必须为：核心OS -> 共享库 -> 分区 [cite: 34, 35]
        baos.write(generateCoreOsSpaceDomainDescriptor(module, fieldMap));
        baos.write(generateSharedLibrarySpaceDomainDescriptors(module, fieldMap));
        baos.write(generatePartitionSpaceDomainDescriptors(module, fieldMap));

        return baos.toByteArray();
    }

    // =======================================================================
    // 各个空间域描述信息存储结构的生成方法
    // =======================================================================

    /**
     * generateCoreOsSpaceDomainDescriptor
     *
     * @brief
     * 功能: 4.1.11.1 核心OS空间域描述信息存储结构(类型0) - 表50。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCoreOsSpaceDomainDescriptor(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CoreOsSpaceDomainDescriptor_";
        String domainName = module.getfCoreOS().getKernelNameAsString();

        // 1. 记录大小 (unsigned int) - 4字节
        // 32位计算公式: (111 + 域名称的长度) & 0xfffffff8
        int recordSize = (111 + domainName.length()) & 0xFFFFFFF8;
        baos.write(intToBytesLittleEndian(recordSize));
        fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

        // 2. 版本号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

        // 3. 域名 (unsigned int) - 4字节，32位固定值0x60
        baos.write(intToBytesLittleEndian(0x60));
        fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x60));

        // 4. 序号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

        // 5. 组名 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

        // 6. 访问权限 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

        // 7. 域地址 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "DomainAddress", longTo4BytesLittleEndian(0L));

        // 8. 域大小 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "DomainSize", longTo4BytesLittleEndian(0L));

        // 9. 连接表地址 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "LinkTableAddress", longTo4BytesLittleEndian(0L));

        // 10. 连接表大小 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "LinkTableSize", longTo4BytesLittleEndian(0L));

        // 11. 域号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

        // 12. 初始化例程 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "InitRoutine", longTo4BytesLittleEndian(0L));

        // 13. 终止例程 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "StopRoutine", longTo4BytesLittleEndian(0L));

        // 14. 组件初始化链 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "ComponentInitChain", longTo4BytesLittleEndian(0L));

        // 15. 组件初始化例程 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo4BytesLittleEndian(0L));

        // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

        // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

        // 18. 特权模式 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

        // 19. 堆大小 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "HeapSize", longTo4BytesLittleEndian(0L));

        // 20. 页管理信息地址 (unsigned long) - 4字节 (32位)，固定值0
        baos.write(longTo4BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo4BytesLittleEndian(0L));

        // 21. 连接路径地址 (unsigned long) - 4字节 (32位)
        // 32位计算公式: 0x63 + 域名称的长度
        long linkPathAddress = 0x63 + domainName.length();
        baos.write(longTo4BytesLittleEndian(linkPathAddress));
        fieldMap.put(baseFieldName + "LinkPathAddress", longTo4BytesLittleEndian(linkPathAddress));

        // 22-25. 保留位1-4 (unsigned int) - 4*4=16字节，固定值0
        for (int i = 1; i <= 4; i++) {
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "Reserved" + i, intToBytesLittleEndian(0));
        }

        // 26. 域名称 (字符串) - 长度=字符串长度+1
        // 映射XML: /Module/CoreOS/CoreOSDescription/@KernelName
        byte[] domainNameBytes = toNullTerminatedAscii(domainName);
        baos.write(domainNameBytes);
        fieldMap.put(baseFieldName + "DomainNameString", domainNameBytes);

        // 27. 池名称 (字符串) - 长度=字符串长度+1
        // 固定字符串";"
        byte[] poolNameBytes = toNullTerminatedAscii(";");
        baos.write(poolNameBytes);
        fieldMap.put(baseFieldName + "PoolNameString", poolNameBytes);

        // 28. 共享库路径 (字符串) - 长度=字符串长度+1
        // 固定字符串"."
        byte[] sharedLibPathBytes = toNullTerminatedAscii(".");
        baos.write(sharedLibPathBytes);
        fieldMap.put(baseFieldName + "SharedLibraryPath", sharedLibPathBytes);

        // 8字节对齐（记录大小计算已包含对齐）
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    /**
     * generateSharedLibrarySpaceDomainDescriptors
     *
     * @brief
     * 功能: 4.1.11.2 共享库空间域描述信息存储结构 (类型1) - 表51。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateSharedLibrarySpaceDomainDescriptors(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (SharedLibrary lib : module.getfSharedLibraries().values()) {
            String domainName = lib.getfNameAsString();
            String poolName = domainName + "Pool;";
            String sharedLibPath = ".";
            String baseFieldName = "SharedLibrarySpaceDomainDescriptor_" + domainName + "_";

            // 1. 记录大小 (unsigned int) - 4字节
            // 32位计算公式: (115 + (域名称的长度)*2 + 共享库路径的长度) & 0xfffffff8
            int recordSize = (115 + (domainName.length()) * 2 + (sharedLibPath.length())) & 0xFFFFFFF8;
            baos.write(intToBytesLittleEndian(recordSize));
            fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

            // 2. 版本号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

            // 3. 域名 (unsigned int) - 4字节，32位固定值0x60
            baos.write(intToBytesLittleEndian(0x60));
            fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x60));

            // 4. 序号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

            // 5. 组名 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

            // 6. 访问权限 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

            // 7. 域地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainAddress", longTo4BytesLittleEndian(0L));

            // 8. 域大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainSize", longTo4BytesLittleEndian(0L));

            // 9. 连接表地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableAddress", longTo4BytesLittleEndian(0L));

            // 10. 连接表大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableSize", longTo4BytesLittleEndian(0L));

            // 11. 域号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

            // 12. 初始化例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "InitRoutine", longTo4BytesLittleEndian(0L));

            // 13. 终止例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "StopRoutine", longTo4BytesLittleEndian(0L));

            // 14. 组件初始化链 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitChain", longTo4BytesLittleEndian(0L));

            // 15. 组件初始化例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo4BytesLittleEndian(0L));

            // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

            // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

            // 18. 特权模式 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

            // 19. 堆大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "HeapSize", longTo4BytesLittleEndian(0L));

            // 20. 页管理信息地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo4BytesLittleEndian(0L));

            // 21. 连接路径地址 (unsigned long) - 4字节 (32位)
            // 32位计算公式: 0x67 + (域名称的长度)*2
            long linkPathAddress = 0x67 + (domainName.length()) * 2;
            baos.write(longTo4BytesLittleEndian(linkPathAddress));
            fieldMap.put(baseFieldName + "LinkPathAddress", longTo4BytesLittleEndian(linkPathAddress));

            // 22-25. 保留位1-4 (unsigned int) - 4*4=16字节，固定值0
            for (int i = 1; i <= 4; i++) {
                baos.write(intToBytesLittleEndian(0));
                fieldMap.put(baseFieldName + "Reserved" + i, intToBytesLittleEndian(0));
            }

            // 26. 域名称 (字符串) - 长度=字符串长度+1
            // 映射XML: /Module/SharedLibraryRegions/SharedLibrary/@Name
            byte[] domainNameBytes = toNullTerminatedAscii(domainName);
            baos.write(domainNameBytes);
            fieldMap.put(baseFieldName + "DomainNameString", domainNameBytes);

            // 27. 池名称 (字符串) - 长度=字符串长度+1
            // 映射XML: /Module/SharedLibraryRegions/SharedLibrary/@Name
            // Name属性值与"Pool;"拼接而成
            byte[] poolNameBytes = toNullTerminatedAscii(poolName);
            baos.write(poolNameBytes);
            fieldMap.put(baseFieldName + "PoolNameString", poolNameBytes);

            // 28. 共享库路径 (字符串) - 长度=字符串长度+1
            // 固定字符串"."
            byte[] sharedLibPathBytes = toNullTerminatedAscii(sharedLibPath);
            baos.write(sharedLibPathBytes);
            fieldMap.put(baseFieldName + "SharedLibraryPath", sharedLibPathBytes);

            // 每个共享库都进行8字节对齐（记录大小计算已包含对齐）
            addAlignmentPadding(baos, fieldMap, baseFieldName);
        }

        return baos.toByteArray();
    }

    /**
     * generatePartitionSpaceDomainDescriptors
     *
     * @brief
     * 功能: 4.1.11.3 分区空间域描述信息存储结构 (类型2) - 表52。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePartitionSpaceDomainDescriptors(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Partition partition : module.getfPartitions().values()) {
            // 跳过虚分区（伪分区）
            if (partition.isFIsPseudoPartition()) {
                continue;
            }
            
            String domainName = partition.getfNameAsString();
            String poolName = domainName + "Pool;";

            // 构造共享库路径 - 按照表52规范
            // 当 NameRef 有 1 个时，由"."+ "："+@NameRef 属性拼接而成。
            // 当 NameRef 有 2 个时，"."+"："+@NameRef 属性1+"："+@NameRef 属性2 拼接而成，依次类推。
            StringBuilder sharedLibPathBuilder = new StringBuilder(".");
            if (partition.getfSharedLibraryNameRefs() != null && !partition.getfSharedLibraryNameRefs().isEmpty()) {
                for (String nameRef : partition.getfSharedLibraryNameRefs()) {
                    sharedLibPathBuilder.append(":").append(nameRef);
                }
            }
            String sharedLibPath = sharedLibPathBuilder.toString();

            String baseFieldName = "PartitionSpaceDomainDescriptor_" + domainName + "_";

            // 1. 记录大小 (unsigned int) - 4字节
            // 32位计算公式: (115+ (域名称的长度)*2 +（共享库路径的长度）) & 0xfffffff8
            int recordSize = (115 + (domainName.length()) * 2 + (sharedLibPath.length())) & 0xFFFFFFF8;
            baos.write(intToBytesLittleEndian(recordSize));
            fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

            // 2. 版本号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

            // 3. 域名 (unsigned int) - 4字节，32位固定值0x60
            baos.write(intToBytesLittleEndian(0x60));
            fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x60));

            // 4. 序号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

            // 5. 组名 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

            // 6. 访问权限 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

            // 7. 域地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainAddress", longTo4BytesLittleEndian(0L));

            // 8. 域大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainSize", longTo4BytesLittleEndian(0L));

            // 9. 连接表地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableAddress", longTo4BytesLittleEndian(0L));

            // 10. 连接表大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableSize", longTo4BytesLittleEndian(0L));

            // 11. 域号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

            // 12. 初始化例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "InitRoutine", longTo4BytesLittleEndian(0L));

            // 13. 终止例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "StopRoutine", longTo4BytesLittleEndian(0L));

            // 14. 组件初始化链 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitChain", longTo4BytesLittleEndian(0L));

            // 15. 组件初始化例程 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo4BytesLittleEndian(0L));

            // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

            // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

            // 18. 特权模式 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

            // 19. 堆大小 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "HeapSize", longTo4BytesLittleEndian(0L));

            // 20. 页管理信息地址 (unsigned long) - 4字节 (32位)，固定值0
            baos.write(longTo4BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo4BytesLittleEndian(0L));

            // 21. 连接路径地址 (unsigned long) - 4字节 (32位)
            // 32位计算公式: 0x67 + (域名称的长度)*2
            long linkPathAddress = 0x67 + (domainName.length()) * 2;
            baos.write(longTo4BytesLittleEndian(linkPathAddress));
            fieldMap.put(baseFieldName + "LinkPathAddress", longTo4BytesLittleEndian(linkPathAddress));

            // 22-25. 保留位1-4 (unsigned int) - 4*4=16字节，固定值0
            for (int i = 1; i <= 4; i++) {
                baos.write(intToBytesLittleEndian(0));
                fieldMap.put(baseFieldName + "Reserved" + i, intToBytesLittleEndian(0));
            }

            // 26. 域名称 (字符串) - 长度=字符串长度+1
            // 映射XML: /Module/Partitions/Partition/@Name
            byte[] domainNameBytes = toNullTerminatedAscii(domainName);
            baos.write(domainNameBytes);
            fieldMap.put(baseFieldName + "DomainNameString", domainNameBytes);

            // 27. 池名称 (字符串) - 长度=字符串长度+1
            // 映射XML: /Module/Partitions/Partition/@Name
            // Name属性值与"Pool;"拼接而成
            byte[] poolNameBytes = toNullTerminatedAscii(poolName);
            baos.write(poolNameBytes);
            fieldMap.put(baseFieldName + "PoolNameString", poolNameBytes);

            // 28. 共享库路径 (字符串) - 长度=字符串长度+1
            // 映射XML: /Module/Partitions/Partition/PartitionDescription/SharedLibraryRegion/@NameRef
            // 当NameRef有1个时，由"."+"："+@NameRef属性拼接而成。
            // 当NameRef有2个时，"."+"："+@NameRef属性1+"："+@NameRef属性2拼接而成，依次类推。
            byte[] sharedLibPathBytes = toNullTerminatedAscii(sharedLibPath);
            baos.write(sharedLibPathBytes);
            fieldMap.put(baseFieldName + "SharedLibraryPath", sharedLibPathBytes);

            // 8字节对齐（记录大小计算已包含对齐）
            addAlignmentPadding(baos, fieldMap, baseFieldName);
        }

        return baos.toByteArray();
    }
}
