/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SpaceDomainDescriptorGenerator64small.java
 * 
 * 描述    : 生成Space Domain Descriptor相关64位小端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

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
 * 功能: Space Domain Descriptor配置二进制生成器（64位小端）。
 */
public class SpaceDomainDescriptorGenerator64small {

    // =======================================================================
    // 辅助方法
    // =======================================================================

    /**
     * longTo8BytesLittleEndian
     *
     * @brief
     * 功能: 执行longTo8BytesLittleEndian处理逻辑。
     *
     * @param[in]  value: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] longTo8BytesLittleEndian(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    /**
     * intToBytesLittleEndian
     *
     * @brief
     * 功能: 执行intToBytesLittleEndian处理逻辑。
     *
     * @param[in]  value: 参数
     * @return 处理结果
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
     * 功能: 执行toNullTerminatedAscii处理逻辑。
     *
     * @param[in]  str: 参数
     * @return 处理结果
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
     * 添加8字节对齐填充
     * 
     * @param baos        输出流
     * @param fieldMap    字段映射
     * @param fieldPrefix 字段前缀
     * @throws IOException IO异常
     */
    private void addAlignmentPadding(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap, String fieldPrefix)
            throws IOException {
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
     * 功能: 执行alignTo8Bytes处理逻辑。
     *
     * @param[in]  size: 参数
     * @return 处理结果
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
     * 功能: 执行generateAllSpaceDomainDescriptors处理逻辑。
     *
     * @param[in]  module: 参数
     * @param[in]  fieldMap: 参数
     * @return 处理结果
     * @scope internal
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
     * 功能: 执行generateCoreOsSpaceDomainDescriptor处理逻辑。
     *
     * @param[in]  module: 参数
     * @param[in]  fieldMap: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCoreOsSpaceDomainDescriptor(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CoreOsSpaceDomainDescriptor_";
        String domainName = module.getfCoreOS().getKernelNameAsString();

        // 1. 记录大小 (unsigned int) - 4字节
        // 64位计算公式: (163 + 域名称的长度) & 0xfffffff8
        int recordSize = (163 + domainName.length()) & 0xFFFFFFF8;
        baos.write(intToBytesLittleEndian(recordSize));
        fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

        // 2. 版本号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

        // 3. 域名 (unsigned int) - 4字节，64位固定值0x94
        baos.write(intToBytesLittleEndian(0x94));
        fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x94));

        // 4. 序号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

        // 5. 组名 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

        // 6. 访问权限 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

        // 7. 域地址 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "DomainAddress", longTo8BytesLittleEndian(0L));

        // 8. 域大小 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "DomainSize", longTo8BytesLittleEndian(0L));

        // 9. 连接表地址 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "LinkTableAddress", longTo8BytesLittleEndian(0L));

        // 10. 连接表大小 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "LinkTableSize", longTo8BytesLittleEndian(0L));

        // 11. 域号 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

        // 11.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
        byte[] padding1 = intToBytesLittleEndian(0);
        baos.write(padding1);
        fieldMap.put(baseFieldName + "Padding_After_DomainId", padding1);

        // 12. 初始化例程 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "InitRoutine", longTo8BytesLittleEndian(0L));

        // 13. 终止例程 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "StopRoutine", longTo8BytesLittleEndian(0L));

        // 14. 组件初始化链 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "ComponentInitChain", longTo8BytesLittleEndian(0L));

        // 15. 组件初始化例程 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo8BytesLittleEndian(0L));

        // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

        // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

        // 18. 特权模式 (unsigned int) - 4字节，固定值0
        baos.write(intToBytesLittleEndian(0));
        fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

        // 18.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
        byte[] padding2 = intToBytesLittleEndian(0);
        baos.write(padding2);
        fieldMap.put(baseFieldName + "Padding_After_PrivilegeMode", padding2);

        // 19. 堆大小 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "HeapSize", longTo8BytesLittleEndian(0L));

        // 20. 页管理信息地址 (unsigned long) - 8字节，固定值0
        baos.write(longTo8BytesLittleEndian(0L));
        fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo8BytesLittleEndian(0L));

        // 21. 连接路径地址 (unsigned long) - 8字节
        // 64位计算公式: 0x97 + 域名称的长度
        long linkPathAddress = 0x97 + domainName.length();
        baos.write(longTo8BytesLittleEndian(linkPathAddress));
        fieldMap.put(baseFieldName + "LinkPathAddress", longTo8BytesLittleEndian(linkPathAddress));

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
     * 4.1.11.2 共享库空间域描述信息存储结构 (类型1) - 表51
     * 
     * 64位系统字段说明：
     * - unsigned long 字段占用8字节
     * - unsigned int 字段占用4字节
     * - 记录大小计算：(167 + (域名称长度)*2 + 共享库路径长度) & 0xfffffff8
     * - 连接路径地址计算：0x9b + (域名称长度)*2
     * - 域名称：SharedLibrary/@Name
     * - 池名称：Name + "Pool;"
     * - 共享库路径：固定字符串"."
     */
    private byte[] generateSharedLibrarySpaceDomainDescriptors(Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (SharedLibrary lib : module.getfSharedLibraries().values()) {
            String domainName = lib.getfNameAsString();
            String poolName = domainName + "Pool;";
            String sharedLibPath = ".";
            String baseFieldName = "SharedLibrarySpaceDomainDescriptor_" + domainName + "_";

            // 1. 记录大小 (unsigned int) - 4字节
            // 64位计算公式: (167 + (域名称的长度)*2 + 共享库路径的长度) & 0xfffffff8
            int recordSize = (167 + (domainName.length()) * 2 + (sharedLibPath.length())) & 0xFFFFFFF8;
            baos.write(intToBytesLittleEndian(recordSize));
            fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

            // 2. 版本号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

            // 3. 域名 (unsigned int) - 4字节，64位固定值0x94
            baos.write(intToBytesLittleEndian(0x94));
            fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x94));

            // 4. 序号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

            // 5. 组名 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

            // 6. 访问权限 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

            // 7. 域地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainAddress", longTo8BytesLittleEndian(0L));

            // 8. 域大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainSize", longTo8BytesLittleEndian(0L));

            // 9. 连接表地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableAddress", longTo8BytesLittleEndian(0L));

            // 10. 连接表大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableSize", longTo8BytesLittleEndian(0L));

            // 11. 域号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

            // 11.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
            byte[] padding1 = intToBytesLittleEndian(0);
            baos.write(padding1);
            fieldMap.put(baseFieldName + "Padding_After_DomainId", padding1);

            // 12. 初始化例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "InitRoutine", longTo8BytesLittleEndian(0L));

            // 13. 终止例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "StopRoutine", longTo8BytesLittleEndian(0L));

            // 14. 组件初始化链 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitChain", longTo8BytesLittleEndian(0L));

            // 15. 组件初始化例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo8BytesLittleEndian(0L));

            // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

            // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

            // 18. 特权模式 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

            // 18.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
            byte[] padding2 = intToBytesLittleEndian(0);
            baos.write(padding2);
            fieldMap.put(baseFieldName + "Padding_After_PrivilegeMode", padding2);

            // 19. 堆大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "HeapSize", longTo8BytesLittleEndian(0L));

            // 20. 页管理信息地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo8BytesLittleEndian(0L));

            // 21. 连接路径地址 (unsigned long) - 8字节
            // 64位计算公式: 0x9b + (域名称的长度)*2
            long linkPathAddress = 0x9b + (domainName.length()) * 2;
            baos.write(longTo8BytesLittleEndian(linkPathAddress));
            fieldMap.put(baseFieldName + "LinkPathAddress", longTo8BytesLittleEndian(linkPathAddress));

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
     * 4.1.11.3 分区空间域描述信息存储结构 (类型2) - 表52
     * 
     * 64位系统字段说明：
     * - unsigned long 字段占用8字节
     * - unsigned int 字段占用4字节
     * - 记录大小计算：(167 + (域名称长度)*2 + 共享库路径长度) & 0xfffffff8
     * - 连接路径地址计算：0x9b + (域名称长度)*2
     * - 域名称：Partition/@Name
     * - 池名称：Name + "Pool;"
     * - 共享库路径：按NameRef数量拼接，格式"." + ":" + NameRef1 + ":" + NameRef2...
     */
    private byte[] generatePartitionSpaceDomainDescriptors(Module module, Map<String, byte[]> fieldMap)
            throws IOException {
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
            // 64位计算公式: (167+ (域名称的长度)*2 +（共享库路径的长度）) & 0xfffffff8
            int recordSize = (167 + (domainName.length()) * 2 + (sharedLibPath.length())) & 0xFFFFFFF8;
            baos.write(intToBytesLittleEndian(recordSize));
            fieldMap.put(baseFieldName + "RecordSize", intToBytesLittleEndian(recordSize));

            // 2. 版本号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "Version", intToBytesLittleEndian(0));

            // 3. 域名 (unsigned int) - 4字节，64位固定值0x94
            baos.write(intToBytesLittleEndian(0x94));
            fieldMap.put(baseFieldName + "DomainNameConst", intToBytesLittleEndian(0x94));

            // 4. 序号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "SequenceNumber", intToBytesLittleEndian(0));

            // 5. 组名 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "GroupName", intToBytesLittleEndian(0));

            // 6. 访问权限 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "AccessPermissions", intToBytesLittleEndian(0));

            // 7. 域地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainAddress", longTo8BytesLittleEndian(0L));

            // 8. 域大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "DomainSize", longTo8BytesLittleEndian(0L));

            // 9. 连接表地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableAddress", longTo8BytesLittleEndian(0L));

            // 10. 连接表大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "LinkTableSize", longTo8BytesLittleEndian(0L));

            // 11. 域号 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "DomainId", intToBytesLittleEndian(0));

            // 11.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
            byte[] padding1 = intToBytesLittleEndian(0);
            baos.write(padding1);
            fieldMap.put(baseFieldName + "Padding_After_DomainId", padding1);

            // 12. 初始化例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "InitRoutine", longTo8BytesLittleEndian(0L));

            // 13. 终止例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "StopRoutine", longTo8BytesLittleEndian(0L));

            // 14. 组件初始化链 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitChain", longTo8BytesLittleEndian(0L));

            // 15. 组件初始化例程 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "ComponentInitRoutine", longTo8BytesLittleEndian(0L));

            // 16. 优先级范围（低） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeLow", intToBytesLittleEndian(0));

            // 17. 优先级范围（高） (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PriorityRangeHigh", intToBytesLittleEndian(0));

            // 18. 特权模式 (unsigned int) - 4字节，固定值0
            baos.write(intToBytesLittleEndian(0));
            fieldMap.put(baseFieldName + "PrivilegeMode", intToBytesLittleEndian(0));

            // 18.5. 对齐填充 (4字节) - 确保下一个8字节字段对齐
            byte[] padding2 = intToBytesLittleEndian(0);
            baos.write(padding2);
            fieldMap.put(baseFieldName + "Padding_After_PrivilegeMode", padding2);

            // 19. 堆大小 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "HeapSize", longTo8BytesLittleEndian(0L));

            // 20. 页管理信息地址 (unsigned long) - 8字节，固定值0
            baos.write(longTo8BytesLittleEndian(0L));
            fieldMap.put(baseFieldName + "PageManagementInfoAddress", longTo8BytesLittleEndian(0L));

            // 21. 连接路径地址 (unsigned long) - 8字节
            // 64位计算公式: 0x9b + (域名称的长度)*2
            long linkPathAddress = 0x9b + (domainName.length()) * 2;
            baos.write(longTo8BytesLittleEndian(linkPathAddress));
            fieldMap.put(baseFieldName + "LinkPathAddress", longTo8BytesLittleEndian(linkPathAddress));

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
            // 映射XML:
            // /Module/Partitions/Partition/PartitionDescription/SharedLibraryRegion/@NameRef
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
