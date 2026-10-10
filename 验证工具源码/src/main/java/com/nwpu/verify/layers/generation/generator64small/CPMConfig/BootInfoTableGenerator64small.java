/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: BootInfoTableGenerator64small.java
 * 
 * 描述    : 生成Boot Info Table相关64位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
/**
 * @brief
 * 功能: Boot Info Table配置二进制生成器（64位小端）。
 */
public class BootInfoTableGenerator64small {

    // 固定值常量
    private static final int BOOT_INFO_TABLE_SIZE = 0; // 引导信息表大小：固定值正整数"0"
    private static final String MAGIC_STRING = "BIT"; // 魔术字："BIT"
    private static final int MAGIC_STRING_SIZE = 4; // 魔术字固定长度：4字节

    /**
     * 生成引导信息表配置存储结构的二进制数据
     * 
     * @param module 模块对象
     * @param fieldMap 字段映射，用于记录生成的二进制数据
     * @param cpmStartAddress CPM配置信息可加载文件起始地址
     * @param spaceDomainDescriptorOffset 空间域描述信息存储结构在CPM配置信息可加载文件中的偏移
     * @param memoryManagementEntryOffset 内存管理入口定义存储结构在CPM配置信息可加载文件中的偏移
     * @return 引导信息表配置存储结构的二进制数据
     * @throws IOException IO异常
     */
    public static byte[] generateBootInfoTable(Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress,
            long spaceDomainDescriptorOffset,
            long memoryManagementEntryOffset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 字段 1: 引导信息表大小 (4字节) - 固定值正整数"0"
        byte[] bootInfoTableSizeBytes = BinaryUtils.intToBytesLittleEndian(BOOT_INFO_TABLE_SIZE);
        baos.write(bootInfoTableSizeBytes);
        fieldMap.put("BootInfoTableSize", bootInfoTableSizeBytes);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "BootInfoTable_Padding_After1");

        // 字段 2: 空间域描述地址 (8字节)
        // CPM配置信息可加载文件起始地址+空间域描述信息存储结构在CPM配置信息可加载文件中的偏移
        long spaceDomainDescriptorAddress = cpmStartAddress + spaceDomainDescriptorOffset;
        byte[] spaceDomainDescriptorAddressBytes = BinaryUtils.longTo8BytesLittleEndian(spaceDomainDescriptorAddress);
        baos.write(spaceDomainDescriptorAddressBytes);
        fieldMap.put("SpaceDomainDescriptorAddress", spaceDomainDescriptorAddressBytes);

        // 字段 3: 空间域描述个数 (4字节)
        // 空间域描述信息存储结构中空间域描述的个数
        int spaceDomainCount = calculateSpaceDomainCount(module);
        byte[] spaceDomainCountBytes = BinaryUtils.intToBytesLittleEndian(spaceDomainCount);
        baos.write(spaceDomainCountBytes);
        fieldMap.put("SpaceDomainCount", spaceDomainCountBytes);

        // 在8字节字段前检查对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "BootInfoTable_Padding_After3");

        // 字段 4: 内存管理入口定义地址 (8字节)
        // CPM配置信息可加载文件起始地址+内存管理入口定义存储结构在CPM配置信息可加载文件中的偏移
        long memoryManagementEntryAddress = cpmStartAddress + memoryManagementEntryOffset;
        byte[] memoryManagementEntryAddressBytes = BinaryUtils.longTo8BytesLittleEndian(memoryManagementEntryAddress);
        baos.write(memoryManagementEntryAddressBytes);
        fieldMap.put("MemoryManagementEntryAddress", memoryManagementEntryAddressBytes);

        // 字段 5: 内存管理记录个数 (4字节)
        // 内存管理入口定义的个数
        int memoryManagementRecordCount = calculateMemoryManagementRecordCount(module);
        byte[] memoryManagementRecordCountBytes = BinaryUtils.intToBytesLittleEndian(memoryManagementRecordCount);
        baos.write(memoryManagementRecordCountBytes);
        fieldMap.put("MemoryManagementRecordCount", memoryManagementRecordCountBytes);

        // 字段 6: 魔术字 (4字节)
        // "BIT"，取值长度不满4时以字符"\0"填充直至长度达到4字节
        byte[] magicStringBytes = BinaryUtils.fixedLengthAscii(MAGIC_STRING, MAGIC_STRING_SIZE, '\0');
        baos.write(magicStringBytes);
        fieldMap.put("MagicString", magicStringBytes);

        // 最终确保整个结构8字节对齐
        add8ByteAlignmentIfNeeded(baos, fieldMap, "BootInfoTable_Padding_Final");

        byte[] allBytes = baos.toByteArray();

        return allBytes;
    }
    /**
     * calculateSpaceDomainCount
     *
     * @brief
     * 功能: 执行calculateSpaceDomainCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSpaceDomainCount(Module module) {
        int count = 0;

        // 共享库空间域描述
        if (module.getfSharedLibraries() != null) {
            count += module.getfSharedLibraries().size();
        }

        // 分区空间域描述
        if (module.getfPartitions() != null) {
            count += module.getfPartitions().size();
        }

        return count;
    }
    /**
     * calculateMemoryManagementRecordCount
     *
     * @brief
     * 功能: 执行calculateMemoryManagementRecordCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateMemoryManagementRecordCount(Module module) {
        return 1; // 目前只有一个内存管理记录，即核心存储区配置    
    }
    /**
     * calculateSize
     *
     * @brief
     * 功能: 执行calculateSize处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        int size = 0;

        // 字段1: 引导信息表大小 (4字节)
        size += 4;
        // 检查对齐 (字段1后)
        size = addPaddingIfNeeded(size);

        // 字段2: 空间域描述地址 (8字节)
        size += 8;

        // 字段3: 空间域描述个数 (4字节)
        size += 4;
        // 检查对齐 (字段3后)
        size = addPaddingIfNeeded(size);

        // 字段4: 内存管理入口定义地址 (8字节)
        size += 8;

        // 字段5: 内存管理记录个数 (4字节)
        size += 4;

        // 字段6: 魔术字 (4字节)
        size += 4;

        // 最终对齐
        size = alignTo8Bytes(size);

        return size;
    }
    /**
     * 在8字节字段前检查是否需要padding来达到8字节对齐
     * 如果需要，添加padding
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
     * validateMagicString
     *
     * @brief
     * 功能: 执行validateMagicString处理逻辑。
     *
     * @param[in]  magicBytes: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static boolean validateMagicString(byte[] magicBytes) {
        if (magicBytes == null || magicBytes.length != MAGIC_STRING_SIZE) {
            return false;
        }

        // 提取字符串部分（去除填充的\0）
        int actualLength = 0;
        for (int i = 0; i < magicBytes.length; i++) {
            if (magicBytes[i] == 0) {
                break;
            }
            actualLength++;
        }

        String extractedString = new String(magicBytes, 0, actualLength);
        return MAGIC_STRING.equals(extractedString);
    }
    /**
     * getMagicString
     *
     * @brief
     * 功能: 执行getMagicString处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static String getMagicString() {
        return MAGIC_STRING;
    }
}
