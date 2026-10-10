/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: StorageRegionConfigGenerator64small.java
 * 
 * 描述    : 生成StorageRegionConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.*;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 存储区配置生成器（32位大端序）负责生成存储区配置存储结构的二进制内容。
 */
public class StorageRegionConfigGenerator64small {

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算存储区配置结构的大小（32位版本）存储区配置包括三个结构：1. 核心存储区配置（kernelMem）：物理地址(4) + 逻辑地址(4) + 大小(4) + 名称(36) + 保留位(4) = 52字节（需8字节对齐到56字节）2. 用户存储区配置（userMem）：物理地址(4) + 逻辑地址(4) + 大小(4) + 名称(36) + 保留位(4) = 52字节（需8字节对齐到56字节）3. 空存储区配置：物理地址(4) + 逻辑地址(4) + 大小(4) + 名称(36) + 保留位(4) = 52字节（需8字节对齐到56字节）总计：3 * 56 = 168字节。
     *
     * @param[in]  module: 包含配置信息的Module对象
     * @return 存储区配置结构的总大小
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        return 3 * 64; // 三个存储区结构，每个52字节对齐到56字节
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
    private static void addAlignmentPadding(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap,
            String fieldPrefix) throws IOException {
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
    private static int alignTo8Bytes(int size) {
        return (size + 7) & ~7;
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成存储区配置存储结构的二进制内容。
     *
     * @param[in]  module: 包含所有配置信息的Module对象
     * @param[in]  fieldMap: 用于记录字段映射
     * @return 存储区配置存储结构的完整二进制字节数组
     * @throws IOException 写入流时可能发生异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CoreOS coreOS = module.getfCoreOS();
        if (coreOS == null) {
            System.err.println("警告：CoreOS为null，无法生成存储区配置结构");
            return baos.toByteArray();
        }

        CoreOS.HardwareConfiguration hardwareConfig = coreOS.getfHardwareConfiguration();
        if (hardwareConfig == null) {
            System.err.println("警告：HardwareConfiguration为null，无法生成存储区配置结构");
            return baos.toByteArray();
        }

        // ============= 1. 核心存储区配置（kernelMem）- 表45 =============

        CoreOS.HardwareConfiguration.PhysicalMemory physicalMemory = hardwareConfig.getfPhysicalMemory();
        if (physicalMemory != null) {
            // 物理地址：BaseAddress (unsigned long - 32位4字节)
            long kernelPhysicalAddr = physicalMemory.getfBaseAddress();
            byte[] kernelPhysAddrBytes = BinaryUtils.longTo8BytesLittleEndian(kernelPhysicalAddr);
            baos.write(kernelPhysAddrBytes);
            fieldMap.put("KernelMem_PhysicalAddress", kernelPhysAddrBytes);

            // 逻辑地址：KernelConfiguration/@kernelVirtualAddress (unsigned long - 64位8字节)
            long kernelLogicalAddr = coreOS.getfKernelConfiguration() == null
                    ? 0L
                    : coreOS.getfKernelConfiguration().getfKernelVirtualAddress();
            byte[] kernelLogAddrBytes = BinaryUtils.longTo8BytesLittleEndian(kernelLogicalAddr);
            baos.write(kernelLogAddrBytes);
            fieldMap.put("KernelMem_LogicalAddress", kernelLogAddrBytes);

            // 大小：KernelMemoryRegionSize (unsigned long - 32位4字节)
            long kernelSize = physicalMemory.getfSize();
            byte[] kernelSizeBytes = BinaryUtils.longTo8BytesLittleEndian(kernelSize);
            baos.write(kernelSizeBytes);
            fieldMap.put("KernelMem_Size", kernelSizeBytes);

            // 名称：固定为"kernelMem" (36字节)
            byte[] kernelNameBytes = BinaryUtils.fixedLengthAscii("kernelMem", 36, '\0');
            baos.write(kernelNameBytes);
            fieldMap.put("KernelMem_Name", kernelNameBytes);

            // 保留位：固定为0x11
            int kernelReserved = 0x11;
            byte[] kernelReservedBytes = BinaryUtils.intToBytesLittleEndian(kernelReserved);
            baos.write(kernelReservedBytes);
            fieldMap.put("KernelMem_Reserved", kernelReservedBytes);

            // 8字节对齐 (当前52字节，对齐到56字节)
            addAlignmentPadding(baos, fieldMap, "KernelMem_");

        } else {
            System.err.println("警告：PhysicalMemory为null，使用默认值");
            // 使用默认值填充核心存储区
            generateDefaultStorageRegion(baos, fieldMap, "KernelMem", "kernelMem", 0x11);
        }

        // ============= 2. 用户存储区配置（userMem）- 表46 =============        
        CoreOS.HardwareConfiguration.UserMemoryRegion userMemory = hardwareConfig.getfUserMemoryRegion();
        if (userMemory != null) {
            // 物理地址：UserMemoryRegion的BaseAddress (unsigned long - 32位4字节)
            long userPhysicalAddr = userMemory.getfBaseAddress();
            byte[] userPhysAddrBytes = BinaryUtils.longTo8BytesLittleEndian(userPhysicalAddr);
            baos.write(userPhysAddrBytes);
            fieldMap.put("UserMem_PhysicalAddress", userPhysAddrBytes);

            // 逻辑地址：与物理地址相同 (unsigned long - 32位4字节)
            long userLogicalAddr = userPhysicalAddr;
            byte[] userLogAddrBytes = BinaryUtils.longTo8BytesLittleEndian(userLogicalAddr);
            baos.write(userLogAddrBytes);
            fieldMap.put("UserMem_LogicalAddress", userLogAddrBytes);

            // 大小：UserMemoryRegion的Size (unsigned long - 32位4字节)
            long userSize = userMemory.getfSize();
            byte[] userSizeBytes = BinaryUtils.longTo8BytesLittleEndian(userSize);
            baos.write(userSizeBytes);
            fieldMap.put("UserMem_Size", userSizeBytes);

            // 名称：固定为"userMem" (36字节)
            byte[] userNameBytes = BinaryUtils.fixedLengthAscii("userMem", 36, '\0');
            baos.write(userNameBytes);
            fieldMap.put("UserMem_Name", userNameBytes);

            // 保留位：固定为0x11
            int userReserved = 0x11;
            byte[] userReservedBytes = BinaryUtils.intToBytesLittleEndian(userReserved);
            baos.write(userReservedBytes);
            fieldMap.put("UserMem_Reserved", userReservedBytes);

            // 8字节对齐 (当前52字节，对齐到56字节)
            addAlignmentPadding(baos, fieldMap, "UserMem_");

        } else {
            System.err.println("警告：UserMemoryRegion为null，使用默认值");
            // 使用默认值填充用户存储区
            generateDefaultStorageRegion(baos, fieldMap, "UserMem", "userMem", 0x11);
        }

        // ============= 3. 空存储区配置 - 表47 =============        
        // 物理地址：-1（全F）(unsigned long - 32位4字节)
        long emptyPhysicalAddr = -1; // 0xFFFFFFFF
        byte[] emptyPhysAddrBytes = BinaryUtils.longTo8BytesLittleEndian(emptyPhysicalAddr);
        baos.write(emptyPhysAddrBytes);
        fieldMap.put("EmptyStorage_PhysicalAddress", emptyPhysAddrBytes);

        // 逻辑地址：-1（全F）(unsigned long - 32位4字节)
        long emptyLogicalAddr = -1; // 0xFFFFFFFF
        byte[] emptyLogAddrBytes = BinaryUtils.longTo8BytesLittleEndian(emptyLogicalAddr);
        baos.write(emptyLogAddrBytes);
        fieldMap.put("EmptyStorage_LogicalAddress", emptyLogAddrBytes);

        // 大小：0 (unsigned long - 32位4字节)
        long emptySize = 0;
        byte[] emptySizeBytes = BinaryUtils.longTo8BytesLittleEndian(emptySize);
        baos.write(emptySizeBytes);
        fieldMap.put("EmptyStorage_Size", emptySizeBytes);

        // 名称：空字符串 (36字节全0)
        byte[] emptyNameBytes = BinaryUtils.fixedLengthAscii("", 36, '\0');
        baos.write(emptyNameBytes);
        fieldMap.put("EmptyStorage_Name", emptyNameBytes);

        // 保留位：0xFFFFFFFF
        int emptyReserved = 0xFFFFFFFF;
        byte[] emptyReservedBytes = BinaryUtils.intToBytesLittleEndian(emptyReserved);
        baos.write(emptyReservedBytes);
        fieldMap.put("EmptyStorage_Reserved", emptyReservedBytes);

        // 8字节对齐 (当前52字节，对齐到56字节)
        addAlignmentPadding(baos, fieldMap, "EmptyStorage_");

        return baos.toByteArray();
    }

    /**
     * generateDefaultStorageRegion
     *
     * @brief
     * 功能: 生成默认的存储区配置（当相关数据为空时使用）。
     *
     * @param[in]  baos: 输出流
     * @param[in]  fieldMap: 字段映射
     * @param[in]  prefix: 字段前缀
     * @param[in]  name: 存储区名称
     * @param[in]  reserved: 保留位值
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static void generateDefaultStorageRegion(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap,
            String prefix, String name, int reserved) throws IOException {
        // 默认物理地址：0 (unsigned long - 32位4字节)
        byte[] physAddrBytes = BinaryUtils.longTo8BytesLittleEndian(0);
        baos.write(physAddrBytes);
        fieldMap.put(prefix + "_PhysicalAddress", physAddrBytes);

        // 默认逻辑地址：0 (unsigned long - 32位4字节)
        byte[] logAddrBytes = BinaryUtils.longTo8BytesLittleEndian(0);
        baos.write(logAddrBytes);
        fieldMap.put(prefix + "_LogicalAddress", logAddrBytes);

        // 默认大小：0 (unsigned long - 32位4字节)
        byte[] sizeBytes = BinaryUtils.longTo8BytesLittleEndian(0);
        baos.write(sizeBytes);
        fieldMap.put(prefix + "_Size", sizeBytes);

        // 名称 (与其他地方保持一致，使用36字节)
        byte[] nameBytes = BinaryUtils.fixedLengthAscii(name, 36, '\0');
        baos.write(nameBytes);
        fieldMap.put(prefix + "_Name", nameBytes);

        // 保留位
        byte[] reservedBytes = BinaryUtils.intToBytesLittleEndian(reserved);
        baos.write(reservedBytes);
        fieldMap.put(prefix + "_Reserved", reservedBytes);

        // 8字节对齐 (当前52字节，对齐到56字节)
        addAlignmentPadding(baos, fieldMap, prefix + "_");
    }
}
