/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MemoryManagerEntryGenerator64small.java
 * 
 * 描述    : 生成MemoryManagerEntry相关32位大端二进制配置数据。
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
 * 功能: 内存管理入口定义二进制生成器 (32位大端)。
 */
public class MemoryManagerEntryGenerator64small {

    // 固定值常量 - 根据表49规格，所有字段都是固定值正整数"0"
    private static final int DOMAIN_ID = 0; // 域ID：固定值正整数"0" (4字节)
    private static final long LOGICAL_ADDRESS = 0L; // 逻辑地址：固定值正整数"0" (32位为4字节)
    private static final long PHYSICAL_ADDRESS = 0L; // 物理地址：固定值正整数"0" (8字节)
    private static final int MEMORY_SIZE = 0; // 内存大小：固定值正整数"0" (4字节)
    private static final int MEMORY_ATTRIBUTE = 0; // 内存属性：固定值正整数"0" (4字节)

    /**
     * generateSingleMemoryManagerEntry
     *
     * @brief
     * 功能: 生成单个内存管理入口定义存储结构的二进制数据。
     *
     * @return 单个内存管理入口定义存储结构的二进制数据
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateSingleMemoryManagerEntry() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 字段 1: 域ID (4字节) - 固定值正整数"0"
        baos.write(BinaryUtils.intToBytesLittleEndian(DOMAIN_ID));

        // 字段 2: 逻辑地址 (4字节) - 固定值正整数"0" (32位系统使用4字节 unsigned long)
        baos.write(BinaryUtils.longTo8BytesLittleEndian(LOGICAL_ADDRESS));

        // 字段 3: 物理地址 (8字节) - 固定值正整数"0" (unsigned long long)
        baos.write(BinaryUtils.longTo8BytesLittleEndian(PHYSICAL_ADDRESS));

        // 字段 4: 内存大小 (4字节) - 固定值正整数"0"
        baos.write(BinaryUtils.intToBytesLittleEndian(MEMORY_SIZE));

        // 字段 5: 内存属性 (4字节) - 固定值正整数"0"
        baos.write(BinaryUtils.intToBytesLittleEndian(MEMORY_ATTRIBUTE));

        // 当前共24字节，已经是8字节对齐，无需填充
        byte[] padding = new byte[4];
        baos.write(padding);

        return baos.toByteArray();
    }

    /**
     * generateMemoryManagerEntries
     *
     * @brief
     * 功能: 生成所有内存管理入口定义存储结构的二进制数据。
     *
     * @param[in]  module: 模块对象
     * @param[in]  fieldMap: 字段映射，用于记录生成的二进制数据
     * @return 所有内存管理入口定义存储结构的二进制数据
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateMemoryManagerEntries(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 计算需要生成的内存管理入口定义个数
        int entryCount = calculateMemoryManagerEntryCount(module);

        // 为每个入口定义生成相同的固定值结构
        for (int i = 0; i < entryCount; i++) {
            byte[] singleEntryBytes = generateSingleMemoryManagerEntry();
            baos.write(singleEntryBytes);

            // 记录到fieldMap中，便于后续对比
            String fieldKey = String.format("MemoryManagerEntry_%d", i);
            fieldMap.put(fieldKey, singleEntryBytes);
        }

        byte[] allEntriesBytes = baos.toByteArray();

        return allEntriesBytes;
    }

    /**
     * calculateMemoryManagerEntryCount
     *
     * @brief
     * 功能: 计算内存管理入口定义的个数。
     *
     * @param[in]  module: 模块对象
     * @return 内存管理入口定义个数
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateMemoryManagerEntryCount(Module module) {
        int count = 0;

        // 核心OS通常有一个内存管理入口 TODO：内存管理入口个数怎么得到
        if (module.getfCoreOS() != null) {
            count++; // 核心OS的内存管理入口
        }

        return count;
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算内存管理入口定义存储结构的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        int entryCount = calculateMemoryManagerEntryCount(module);
        int singleEntrySize = calculateSingleEntrySize();
        return entryCount * singleEntrySize;
    }

    /**
     * calculateSingleEntrySize
     *
     * @brief
     * 功能: 计算单个内存管理入口定义存储结构的大小。
     *
     * @return 单个结构的大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleEntrySize() {
        // 32位系统：域ID(4) + 逻辑地址(4) + 物理地址(8) + 内存大小(4) + 内存属性(4) = 24字节（已8字节对齐）
        return 4 + 8 + 8 + 4 + 4 + 4;
    }

    /**
     * validateMemoryManagerEntry
     *
     * @brief
     * 功能: 验证内存管理入口定义数据的有效性。
     *
     * @param[in]  entryBytes: 内存管理入口定义的字节数据
     * @return 是否为有效的内存管理入口定义
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean validateMemoryManagerEntry(byte[] entryBytes) {
        if (entryBytes == null || entryBytes.length != calculateSingleEntrySize()) {
            return false;
        }

        // 由于所有字段都应该是固定值0，我们可以验证整个数组是否都为0
        for (byte b : entryBytes) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * getDomainId
     *
     * @brief
     * 功能: 获取固定的域ID值。
     *
     * @return 域ID值
     * @scope external
     * @errno:
     * N/A
     */
    public static int getDomainId() {
        return DOMAIN_ID;
    }

    /**
     * getLogicalAddress
     *
     * @brief
     * 功能: 获取固定的逻辑地址值。
     *
     * @return 逻辑地址值（32位系统为4字节 unsigned long）
     * @scope external
     * @errno:
     * N/A
     */
    public static long getLogicalAddress() {
        return LOGICAL_ADDRESS;
    }

    /**
     * getPhysicalAddress
     *
     * @brief
     * 功能: 获取固定的物理地址值。
     *
     * @return 物理地址值
     * @scope external
     * @errno:
     * N/A
     */
    public static long getPhysicalAddress() {
        return PHYSICAL_ADDRESS;
    }

    /**
     * getMemorySize
     *
     * @brief
     * 功能: 获取固定的内存大小值。
     *
     * @return 内存大小值
     * @scope external
     * @errno:
     * N/A
     */
    public static int getMemorySize() {
        return MEMORY_SIZE;
    }

    /**
     * getMemoryAttribute
     *
     * @brief
     * 功能: 获取固定的内存属性值。
     *
     * @return 内存属性值
     * @scope external
     * @errno:
     * N/A
     */
    public static int getMemoryAttribute() {
        return MEMORY_ATTRIBUTE;
    }
}