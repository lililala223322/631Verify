/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MemoryBlockConfigMasterGenerator64small.java
 * 
 * 描述    : 生成Memory Block Master相关64位小端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.CoreOS;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.entity.SharedLibrary;
import com.nwpu.verify.entity.SharedData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.HashMap; // Needed for the global state map
/**
 * @brief
 * 功能: Memory Block Master配置二进制生成器（64位小端）。
 */
public class MemoryBlockConfigMasterGenerator64small {

    // =======================================================================
    // 辅助方法
    // =======================================================================

    /**
     * longTo4BytesLittleEndian
     *
     * @brief
     * 功能: 执行longTo4BytesLittleEndian处理逻辑。
     *
     * @param[in]  value: 参数
     * @return 处理结果
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
     * fixedLengthAscii
     *
     * @brief
     * 功能: 执行fixedLengthAscii处理逻辑。
     *
     * @param[in]  str: 参数
     * @param[in]  length: 参数
     * @param[in]  padChar: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] fixedLengthAscii(String str, int length, char padChar) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) padChar;
        }
        if (str != null) {
            byte[] src = str.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, bytes, 0, Math.min(src.length, length));
        }
        return bytes;
    }
    /**
     * parseHexOrDecLong
     *
     * @brief
     * 功能: 执行parseHexOrDecLong处理逻辑。
     *
     * @param[in]  s: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private long parseHexOrDecLong(String s) {
        if (s == null || s.isEmpty())
            return 0L;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            try {
                return Long.parseLong(s.substring(2), 16);
            } catch (NumberFormatException e) {
                System.err.println(
                        "Warning: parseHexOrDecLong failed for '" + s + "', returning 0. Error: " + e.getMessage());
                return 0L;
            }
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            System.err.println(
                    "Warning: parseHexOrDecLong failed for '" + s + "', returning 0. Error: " + e.getMessage());
            return 0L;
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

    // =======================================================================
    // 总体大小计算方法
    // =======================================================================
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
        MemoryBlockConfigMasterGenerator64small generator = new MemoryBlockConfigMasterGenerator64small();
        int totalSize = 0;

        // 单个结构的基础大小（每个都是76字节，8字节对齐后为80字节）
        int singleStructSize = 76;
        int alignedStructSize = generator.alignTo8Bytes(singleStructSize); // alignTo8Bytes(76) = 80

        // ==================== 权限配置部分（表22-32）====================

        // 1. 核心内存块权限配置存储结构 (表22) - 1个，总是生成
        totalSize += alignedStructSize;

        // 2. 配置记录内存块权限配置存储结构 (表23) - 1个，总是生成
        totalSize += alignedStructSize;

        // 3. 核心页内存块权限配置存储结构 (表24) - 1个，总是生成
        totalSize += alignedStructSize;

        // 4. 端口缓冲区内存块权限配置存储结构 (表25) - 1个，总是生成
        totalSize += alignedStructSize;

        // 5. 健康监控日志记录内存块权限配置存储结构 (表26) - 1个，总是生成
        totalSize += alignedStructSize;

        // 6. RAM 部署空间内存块权限配置存储结构 (表27) - 1个，总是生成（注意：生成方法中没有if判断）
        totalSize += alignedStructSize;

        // 7. 内核IO空间内存块权限配置存储结构 (表28) - 1个，总是生成
        totalSize += alignedStructSize;

        // 8. 分区内存块权限配置存储结构 (表29) - 每个真实分区1个（每个单独对齐）
        int partitionCount = generator.getRealPartitionCount(module);
        int partitionPermSize = partitionCount * alignedStructSize;
        totalSize += partitionPermSize;

        // 9. 分区操作系统共享库内存块权限配置存储结构 (表30) - 每个共享库1个（每个单独对齐）
        int sharedLibraryCount = generator.getSharedLibraryCount(module);
        int sharedLibPermSize = sharedLibraryCount * alignedStructSize;
        totalSize += sharedLibPermSize;

        // 10. 共享数据内存块权限配置存储结构 (表31) - 每个共享数据1个（每个单独对齐）
        // 注意：生成方法总是被调用，只是如果数量为0则返回空字节数组
        int sharedDataCount = generator.getSharedDataCount(module);
        int sharedDataPermSize = sharedDataCount * alignedStructSize;
        totalSize += sharedDataPermSize;

        // 11. 自定义核心池内存块权限配置存储结构 (表32) - 每个核心池1个
        // 注意：生成方法总是被调用，内部按实际数量生成，最后有一次整体对齐
        int customCorePoolCount = generator.getCustomCorePoolCount(module);
        if (customCorePoolCount > 0) {
            int customCorePoolsAlignedSize = customCorePoolCount * alignedStructSize;
            totalSize += customCorePoolsAlignedSize;
        }

        // ==================== 空间配置部分（表33-44）====================

        // 12. 核心内存块空间配置存储结构 (表33) - 1个，总是生成
        totalSize += alignedStructSize;

        // 13. 核心页内存块空间配置存储结构 (表34) - 1个，总是生成
        totalSize += alignedStructSize;

        // 14. 端口缓冲区内存块空间配置存储结构 (表35) - 1个，总是生成
        totalSize += alignedStructSize;

        // 15. 健康监控日志记录内存块空间配置存储结构 (表36) - 1个，总是生成
        totalSize += alignedStructSize;

        // 16. 自定义核心池内存块空间配置存储结构 (表37) - 可选，有if判断
        if (customCorePoolCount > 0) {
            int customCorePoolsAlignedSize = customCorePoolCount * alignedStructSize;
            totalSize += customCorePoolsAlignedSize;
        }

        // 17. RAM 部署空间内存块空间配置存储结构 (表38) - 可选，有if判断
        if (generator.hasRamPayloadRegion(module)) {
            totalSize += alignedStructSize;
        }

        // 18. 内核IO空间内存块空间配置存储结构 (表39) - 每个KernelIO 1个（每个单独对齐）
        int kernelIOCount = generator.getKernelIOCount(module);
        int kernelIOSpaceSize = kernelIOCount * alignedStructSize;
        totalSize += kernelIOSpaceSize;

        // 19. 配置记录内存块空间配置存储结构 (表40) - 1个，总是生成
        totalSize += alignedStructSize;

        // 20. 分区操作系统共享库内存块空间配置存储结构 (表41) - 每个共享库1个（每个单独对齐）
        int sharedLibSpaceSize = sharedLibraryCount * alignedStructSize;
        totalSize += sharedLibSpaceSize;

        // 21. 共享数据内存块空间配置存储结构 (表42) - 可选，有if判断
        // 注意：表42中每个共享数据都单独对齐（第1935行）
        if (generator.hasSharedData(module)) {
            int sharedDataSpaceSize = sharedDataCount * alignedStructSize;
            totalSize += sharedDataSpaceSize;
        }

        // 22. 分区内存块空间配置存储结构 (表43) - 每个真实分区1个（每个单独对齐）
        int partitionSpaceSize = partitionCount * alignedStructSize;
        totalSize += partitionSpaceSize;

        // 23. 空内存块空间配置存储结构 (表44) - 1个，总是生成
        totalSize += alignedStructSize;

        return totalSize;
    }

    // =======================================================================
    // 各个内存块空间配置结构的大小计算方法 (共23个)
    // =======================================================================

    // 4.1.7.1 核心内存块权限配置存储结构 (表22)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCoreMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateCoreMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCoreMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.2 配置记录内存块权限配置存储结构 (表23)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateConfigRecordMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateConfigRecordMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateConfigRecordMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.3 核心页内存块权限配置存储结构 (表24)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCorePageMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateCorePageMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCorePageMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.4 端口缓冲区内存块权限配置存储结构 (表25)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePortBufferMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculatePortBufferMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePortBufferMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.5 健康监控日志记录内存块权限配置存储结构 (表26)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateHmLogMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateHmLogMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateHmLogMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.6 RAM 部署空间内存块权限配置存储结构 (表27)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateRamPayloadMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateRamPayloadMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateRamPayloadMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.7 内核IO空间内存块权限配置存储结构 (表28)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateKernelIOMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateKernelIOMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateKernelIOMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.8 分区内存块权限配置存储结构 (表29)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePartitionMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculatePartitionMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePartitionMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.9 分区操作系统共享库内存块权限配置存储结构 (表30)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePartitionOSSharedLibraryMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculatePartitionOSSharedLibraryMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePartitionOSSharedLibraryMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.10 共享数据内存块权限配置存储结构 (表31)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateSharedDataMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateSharedDataMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateSharedDataMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.11 自定义核心池内存块权限配置存储结构 (表32)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCustomCorePoolMemoryBlockPermissionConfigSize
     *
     * @brief
     * 功能: 执行calculateCustomCorePoolMemoryBlockPermissionConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCustomCorePoolMemoryBlockPermissionConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.12 核心内存块空间配置存储结构 (表33)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCoreMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateCoreMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCoreMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.13 核心页内存块空间配置存储结构 (表34)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCorePageMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateCorePageMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCorePageMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.14 端口缓冲区内存块空间配置存储结构 (表35)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePortBufferMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculatePortBufferMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePortBufferMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.15 健康监控日志记录内存块空间配置存储结构 (表36)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateHmLogMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateHmLogMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateHmLogMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.16 自定义核心池内存块空间配置存储结构 (表37)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateCustomCorePoolMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateCustomCorePoolMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateCustomCorePoolMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.17 RAM 部署空间内存块空间配置存储结构 (表38)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateRamPayloadMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateRamPayloadMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateRamPayloadMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.18 内核IO空间内存块空间配置存储结构 (表39)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateKernelIOMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateKernelIOMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateKernelIOMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.19 配置记录内存块空间配置存储结构 (表40)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateConfigRecordMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateConfigRecordMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateConfigRecordMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.20 分区操作系统共享库内存块空间配置存储结构 (表41)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePartitionOSSharedLibraryMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculatePartitionOSSharedLibraryMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePartitionOSSharedLibraryMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.21 共享数据内存块空间配置存储结构 (表42)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateSharedDataMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateSharedDataMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateSharedDataMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.22 分区内存块空间配置存储结构 (表43)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculatePartitionMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculatePartitionMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculatePartitionMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // 4.1.7.23 空内存块空间配置存储结构 (表44)
    // 64位: 逻辑地址8字节 + 空间大小8字节
    /**
     * calculateEmptyMemoryBlockSpaceConfigSize
     *
     * @brief
     * 功能: 执行calculateEmptyMemoryBlockSpaceConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public int calculateEmptyMemoryBlockSpaceConfigSize() {
        return 36 + 4 + 8 + 8 + 8 + 4 + 4 + 4;
    } // 76 bytes

    // =======================================================================
    // 各个内存块空间配置结构的二进制生成方法 (共23个，部分已实现，部分为占位符)
    // =======================================================================

    // 4.1.7.1 核心内存块权限配置存储结构 (表22)
    /**
     * generateCoreMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateCoreMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCoreMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CoreMemBlockPermConfig_";
        int structSize = calculateCoreMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelMemPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("kernelMemPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，取 PhysicalMemory/@Base_Address
        long physicalAddress = 0L;

        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，取 PhysicalMemory/@Base_Address (64位)
        long logicalAddress = physicalAddress;
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80431"
        int flag = 0x80431; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CORE_MEM_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CORE_MEM_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CORE_MEM_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.2 配置记录内存块权限配置存储结构 (表23)
    /**
     * generateConfigRecordMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateConfigRecordMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateConfigRecordMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "ConfigRecordMemBlockPermConfig_";
        int structSize = calculateConfigRecordMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "configRecordPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("configRecordPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80431"
        int flag = 0x80431; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CONFIG_REC_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CONFIG_REC_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CONFIG_REC_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.3 核心页内存块权限配置存储结构 (表24)
    /**
     * generateCorePageMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateCorePageMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCorePageMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CorePageMemBlockPermConfig_";
        int structSize = calculateCorePageMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("kernelPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80331"
        int flag = 0x80331; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CORE_PAGE_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CORE_PAGE_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CORE_PAGE_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.4 端口缓冲区内存块权限配置存储结构 (表25)
    /**
     * generatePortBufferMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generatePortBufferMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePortBufferMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "PortBufferMemBlockPermConfig_";
        int structSize = calculatePortBufferMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "portPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("portPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80431"
        int flag = 0x80431; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_PORT_BUFFER_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_PORT_BUFFER_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_PORT_BUFFER_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.5 健康监控日志记录内存块权限配置存储结构 (表26)
    /**
     * generateHmLogMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateHmLogMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateHmLogMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "HmLogMemBlockPermConfig_";
        int structSize = calculateHmLogMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "hmlogPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("hmlogPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80431"
        int flag = 0x80431; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_HMLOG_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_HMLOG_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_HMLOG_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.6 RAM 部署空间内存块权限配置存储结构 (表27)
    /**
     * generateRamPayloadMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateRamPayloadMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateRamPayloadMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "RamPayloadMemBlockPermConfig_";
        int structSize = calculateRamPayloadMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "ramPayloadPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("ramPayloadPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x80331"
        int flag = 0x80331; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_RAM_PAYLOAD_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_RAM_PAYLOAD_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_RAM_PAYLOAD_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.7 内核IO空间内存块权限配置存储结构 (表28)
    /**
     * generateKernelIOMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateKernelIOMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateKernelIOMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "KernelIOMemBlockPermConfig_";
        int structSize = calculateKernelIOMemoryBlockPermissionConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelIoPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("kernelIoPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
        long physicalAddress = 0L; // Fixed value from PDF
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
        long logicalAddress = 0L; // Fixed value from PDF
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
        long spaceSize = 0L; // Fixed value from PDF
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x100231"
        int flag = 0x100231; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_KERNEL_IO_PERM_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_KERNEL_IO_PERM_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_KERNEL_IO_PERM_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.8 分区内存块权限配置存储结构 (表29)
    /**
     * generatePartitionMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generatePartitionMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePartitionMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 多个Partition，需要遍历
        if (module.getfPartitions() != null) {
            for (Map.Entry<String, Partition> entry : module.getfPartitions().entrySet()) {
                Partition partition = entry.getValue();
                // 跳过PseudoPartition，这是不需要的
                if (partition.isFIsPseudoPartition()) {
                    continue;
                }

                String baseFieldName = "PartitionMemBlockPermConfig_" + partition.getfNameAsString() + "_"; // 以分区名为前缀
                int structSize = calculatePartitionMemoryBlockPermissionConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，"Name 属性值和”Pool”拼接而成"
                String spaceName = partition.getfNameAsString() + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值“0”
                long physicalAddress = 0L; // Fixed value from PDF
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
                long logicalAddress = 0L; // Fixed value from PDF
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
                long spaceSize = 0L; // Fixed value from PDF
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值"0"
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
                int mmuAttribute = 0; // Fixed value from PDF
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) - 4字节，固定值"0x80341"
                int flag = 0x80341; // Fixed value from PDF
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // Update global calculated data for potential chaining (per partition)
                globalCalculatedData.put("LAST_PARTITION_PERM_PHYS_ADDR_" + partition.getfIdentifier(),
                        physicalAddress);
                globalCalculatedData.put("LAST_PARTITION_PERM_LOG_ADDR_" + partition.getfIdentifier(), logicalAddress);
                globalCalculatedData.put("LAST_PARTITION_PERM_SIZE_" + partition.getfIdentifier(), spaceSize);

                // 8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);
            }
        }
        return baos.toByteArray();
    }

    // 4.1.7.9 分区操作系统共享库内存块权限配置存储结构 (表30)
    /**
     * generatePartitionOSSharedLibraryMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generatePartitionOSSharedLibraryMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePartitionOSSharedLibraryMemoryBlockPermissionConfigStruct(Module module,
            Map<String, byte[]> fieldMap, Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 多个SharedLibrary，需要遍历
        if (module.getfSharedLibraries() != null) {
            for (Map.Entry<String, SharedLibrary> entry : module.getfSharedLibraries().entrySet()) {
                SharedLibrary sharedLibrary = entry.getValue();
                String baseFieldName = "PartitionOSSharedLibMemBlockPermConfig_" + sharedLibrary.getfNameAsString()
                        + "_";
                int structSize = calculatePartitionOSSharedLibraryMemoryBlockPermissionConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，"Name 属性值与“Pool”拼接的字符串"
                String spaceName = sharedLibrary.getfNameAsString() + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值“0”
                long physicalAddress = 0L; // Fixed value from PDF
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
                long logicalAddress = 0L; // Fixed value from PDF
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
                long spaceSize = 0L; // Fixed value from PDF
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值"0"
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
                int mmuAttribute = 0; // Fixed value from PDF
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) - 4字节，固定值"0x80341"
                int flag = 0x80341; // Fixed value from PDF
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // Update global calculated data for potential chaining (per shared library)
                globalCalculatedData.put(
                        "LAST_PARTITION_OS_SHARED_LIB_PERM_PHYS_ADDR_" + sharedLibrary.getfNameAsString(),
                        physicalAddress);
                globalCalculatedData.put(
                        "LAST_PARTITION_OS_SHARED_LIB_PERM_LOG_ADDR_" + sharedLibrary.getfNameAsString(),
                        logicalAddress);
                globalCalculatedData.put("LAST_PARTITION_OS_SHARED_LIB_PERM_SIZE_" + sharedLibrary.getfNameAsString(),
                        spaceSize);

                // 8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);
            }
        }
        return baos.toByteArray();
    }

    // 4.1.7.10 共享数据内存块权限配置存储结构 (表31)
    /**
     * generateSharedDataMemoryBlockPermissionConfigStruct
     *
     * @brief
     * 功能: 执行 generateSharedDataMemoryBlockPermissionConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateSharedDataMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 共享数据区可能存在多个，需要遍历（从Module级别遍历，避免重复生成）
        if (module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty()) {
            for (Map.Entry<String, com.nwpu.verify.entity.SharedData> sharedDataEntry : module.getfSharedDatas()
                    .entrySet()) {
                com.nwpu.verify.entity.SharedData sharedData = sharedDataEntry.getValue();
                String sharedDataName = sharedData.getfNameAsString();

                if (sharedDataName == null || sharedDataName.trim().isEmpty()) {
                    continue; // 跳过没有Name的SharedData
                }

                String baseFieldName = "SharedDataMemBlockPermConfig_" + sharedDataName + "_";
                int structSize = calculateSharedDataMemoryBlockPermissionConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，"SharedData 元素的Name属性值和"Pool"拼接而成"
                String spaceName = sharedDataName + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值"0"
                long physicalAddress = 0L; // Fixed value from PDF
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
                long logicalAddress = 0L; // Fixed value from PDF
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，固定值"0" (64位)
                long spaceSize = 0L; // Fixed value from PDF
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值"0"
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0"
                int mmuAttribute = 0; // Fixed value from PDF
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) -
                // 4字节，当SharedData子元素为SharedDataDescription时，取固定值"0x80341"；当子元素为SharedIODescription时，取固定值"0x100341"
                int flag;
                if (sharedData.getfSharedIODescription() != null) {
                    // 当子元素为SharedIODescription时，使用0x100341
                    flag = 0x100341;
                } else {
                    // 当子元素为SharedDataDescription时（或默认情况），使用0x80341
                    flag = 0x80341;
                }
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // Update global calculated data for potential chaining (per shared data region)
                globalCalculatedData.put("LAST_SHARED_DATA_PERM_PHYS_ADDR_" + sharedDataName, physicalAddress);
                globalCalculatedData.put("LAST_SHARED_DATA_PERM_LOG_ADDR_" + sharedDataName, logicalAddress);
                globalCalculatedData.put("LAST_SHARED_DATA_PERM_SIZE_" + sharedDataName, spaceSize);

                // 8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);
            }
        }
        return baos.toByteArray();
    }

    /**
     * 4.1.7.11 自定义核心池内存块权限配置存储结构 (表32)
     * 
     * 根据XML路径：/Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/kernelRegion/@PoolName
     * 生成自定义核心池内存块权限配置。此结构为可选项，只有当XML中配置了相应的kernelRegion元素时才会生成。
     * 
     * 结构内容（按照表32规范）：
     * - 空间名：PoolName属性值和"Pool"拼接而成，36字节，不足以"\0"填充
     * - 占位符：4字节，固定值0
     * - 物理地址：8字节，固定值0
     * - 逻辑地址：8字节，固定值0 (64位)
     * - 空间大小：8字节，固定值0 (64位)
     * - 页大小：4字节，固定值0
     * - MMU属性：4字节，固定值0
     * - 标记：4字节，固定值0x80341
     */
    private byte[] generateCustomCorePoolMemoryBlockPermissionConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 检查是否存在kernelRegion配置
        // 由于当前实体类结构中可能没有kernelRegion的完整支持，
        // 这里先检查是否有相关的配置信息可以用来生成该结构

        // 检查是否存在kernelRegion配置
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            List<CoreOS.HardwareConfiguration.KernelRegion> kernelRegions = module.getfCoreOS()
                    .getfHardwareConfiguration().getfPhysicalMemory().getfKernelRegions();

            if (kernelRegions != null && !kernelRegions.isEmpty()) {
                for (CoreOS.HardwareConfiguration.KernelRegion kernelRegion : kernelRegions) {
                    String poolName = kernelRegion.getfPoolName();
                    if (poolName == null || poolName.trim().isEmpty()) {
                        continue; // 跳过没有PoolName的kernelRegion
                    }

                    String baseFieldName = "CustomCorePoolMemBlockPermConfig_" + poolName + "_";

                    // 1. 空间名 (36字节) - PoolName属性值和"Pool"拼接而成
                    String spaceName = poolName + "Pool";
                    byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                    baos.write(spaceNameBytes);
                    fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                    // 2. 占位符 (4字节) - 固定值0
                    byte[] placeholderBytes = intToBytesLittleEndian(0);
                    baos.write(placeholderBytes);
                    fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                    // 3. 物理地址 (8字节) - 固定值0
                    byte[] physicalAddressBytes = longTo8BytesLittleEndian(0L);
                    baos.write(physicalAddressBytes);
                    fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                    // 4. 逻辑地址 (8字节) - 固定值0 (64位)
                    byte[] logicalAddressBytes = longTo8BytesLittleEndian(0L);
                    baos.write(logicalAddressBytes);
                    fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                    // 5. 空间大小 (8字节) - 固定值0 (64位)
                    byte[] spaceSizeBytes = longTo8BytesLittleEndian(0L);
                    baos.write(spaceSizeBytes);
                    fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                    // 6. 页大小 (4字节) - 固定值0
                    byte[] pageSizeBytes = intToBytesLittleEndian(0);
                    baos.write(pageSizeBytes);
                    fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                    // 7. MMU属性 (4字节) - 固定值0
                    byte[] mmuAttributeBytes = intToBytesLittleEndian(0);
                    baos.write(mmuAttributeBytes);
                    fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                    // 8. 标记 (4字节) - 固定值0x80341
                    byte[] flagBytes = intToBytesLittleEndian(0x80341);
                    baos.write(flagBytes);
                    fieldMap.put(baseFieldName + "Flag", flagBytes);

                    // 8字节对齐
                    addAlignmentPadding(baos, fieldMap, baseFieldName);
                }
            }
        }

        return baos.toByteArray();
    }

    // 4.1.7.12 核心内存块空间配置存储结构 (表33)
    /**
     * generateCoreMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateCoreMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCoreMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CoreMemBlockSpaceConfig_";
        int structSize = calculateCoreMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelMemPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("kernelMemPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，取 PhysicalMemory/@Base_Address
        long physicalAddress = 0L;
        if (module != null
                && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            physicalAddress = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfBaseAddress();
        }
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，取 KernelConfiguration/@kernelVirtualAddress (64位)
        long logicalAddress = 0L;
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfKernelConfiguration() != null) {
            logicalAddress = module.getfCoreOS().getfKernelConfiguration().getfKernelVirtualAddress();
        }
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，取 PhysicalMemory/kernelMemoryRegion/@Size (64位)
        long spaceSize = 0L;
        if (module != null
                && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            spaceSize = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfKernelMemoryRegionSize();
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x103"
        int mmuAttribute = 0x103; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x8000002"
        int flag = 0x8000002; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CORE_MEM_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CORE_MEM_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CORE_MEM_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.13 核心页内存块空间配置存储结构 (表34)
    /**
     * generateCorePageMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateCorePageMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateCorePageMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "CorePageMemBlockSpaceConfig_";
        int structSize = calculateCorePageMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("kernelPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节
        long physicalAddress = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                        .getfKernelConfigRecordRegionSize() != null) {
            long kernelConfigRecordRegionSizeVal = parseHexOrDecLong(module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfKernelConfigRecordRegionSize());
            long baseAddrVal = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfBaseAddress();
            long kernelMemRegionSizeVal = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfKernelMemoryRegionSize();
            physicalAddress = kernelConfigRecordRegionSizeVal + baseAddrVal + kernelMemRegionSizeVal;
        }
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节 (64位)
        // 内核配置记录池逻辑地址 + 内核配置记录池空间大小
        long logicalAddress = 0L;
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfKernelConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            logicalAddress = module.getfCoreOS().getfKernelConfiguration().getfKernelVirtualAddress()
                    + module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                            .getfKernelMemoryRegionSize()
                    + parseHexOrDecLong(module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                            .getfKernelConfigRecordRegionSize());
        }
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节 (64位)
        long spaceSize = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            spaceSize = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfKernelPgPoolSize();
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值“0”
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值“0”
        int mmuAttribute = 0; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值“0x8000002”
        int flag = 0x8000002; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CORE_PAGE_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CORE_PAGE_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CORE_PAGE_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.14 端口缓冲区内存块空间配置存储结构 (表35)
    /**
     * generatePortBufferMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generatePortBufferMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePortBufferMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "PortBufferMemBlockSpaceConfig_";
        int structSize = calculatePortBufferMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "portPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("portPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节
        long physicalAddress = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            physicalAddress = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfBaseAddress() +
                    module.getfCoreOS()
                            .getfHardwareConfiguration()
                            .getfPhysicalMemory().getfKernelPgPoolSize()
                    +
                    module.getfCoreOS()
                            .getfHardwareConfiguration()
                            .getfPhysicalMemory().getfKernelMemoryRegionSize()
                    +
                    parseHexOrDecLong(module.getfCoreOS()
                            .getfHardwareConfiguration()
                            .getfPhysicalMemory().getfKernelConfigRecordRegionSize());
        }
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节 (64位)
        // 内核页池逻辑起始地址 + 内核页池空间大小
        long logicalAddress = globalCalculatedData.getOrDefault("LAST_CORE_PAGE_SPACE_LOG_ADDR", 0L)
                + globalCalculatedData.getOrDefault("LAST_CORE_PAGE_SPACE_SIZE", 0L);

        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节 (64位)
        long spaceSize = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            spaceSize = module.getfCoreOS()
                    .getfHardwareConfiguration()
                    .getfPhysicalMemory()
                    .getfPortRegionSize();
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x103"
        int mmuAttribute = 0x103; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x28000002"
        int flag = 0x28000002; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_PORT_BUFFER_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_PORT_BUFFER_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_PORT_BUFFER_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // 4.1.7.15 健康监控日志记录内存块空间配置存储结构 (表36)
    /**
     * generateHmLogMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateHmLogMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateHmLogMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "HmLogMemBlockSpaceConfig_";
        int structSize = calculateHmLogMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "hmlogPgPool"，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("hmlogPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节
        long physicalAddress = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            physicalAddress = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfBaseAddress() +
                    module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfKernelPgPoolSize() +
                    module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfKernelMemoryRegionSize() +
                    parseHexOrDecLong(module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                            .getfKernelConfigRecordRegionSize())
                    +
                    module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfPortRegionSize();
        }
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节 (64位)
        // 端口缓冲池逻辑起始地址 + 端口缓冲池空间大小
        long logicalAddress = globalCalculatedData.getOrDefault("LAST_PORT_BUFFER_SPACE_LOG_ADDR", 0L)
                + globalCalculatedData.getOrDefault("LAST_PORT_BUFFER_SPACE_SIZE", 0L);
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节(64位)
        long spaceSize = 0L; // Fixed value from PDF
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            spaceSize = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfHmLogRegionSize();
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        long pageSize = 0L; // Fixed value from PDF
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x103"
        int mmuAttribute = 0x103; // Fixed value from PDF
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x28000002"
        int flag = 0x28000002; // Fixed value from PDF
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_HMLOG_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_HMLOG_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_HMLOG_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    /**
     * 4.1.7.16 自定义核心池内存块空间配置存储结构 (表37)
     * 
     * 根据XML路径：/Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/kernelRegion
     * 生成自定义核心池内存块空间配置。此结构为可选项，只有当XML中配置了kernelRegion元素时才会生成。
     * 当kernelRegion数量为零时，该存储结构不存在。
     * 
     * 结构内容（按照表37规范）：
     * - 空间名：PoolName属性值和"Pool"拼接而成，36字节，不足以"\0"填充
     * - 占位符：4字节，固定值0
     * - 物理地址：8字节，第一个为健康监控日志池物理地址+空间大小，后续为前一个自定义核心池物理地址+空间大小
     * - 逻辑地址：8字节，计算方式与物理地址相同 (64位)
     * - 空间大小：8字节，kernelRegion元素的Size属性值 (64位)
     * - 页大小：4字节，固定值0
     * - MMU属性：4字节，根据WriteProtected属性值计算（0x103或0x101）
     * - 标记：4字节，固定值0x8000002
     */
    private byte[] generateCustomCorePoolMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 检查是否存在kernelRegion配置
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            List<CoreOS.HardwareConfiguration.KernelRegion> kernelRegions = module.getfCoreOS()
                    .getfHardwareConfiguration().getfPhysicalMemory().getfKernelRegions();

            // 如果kernelRegion数量为零，该存储结构不存在
            if (kernelRegions != null && !kernelRegions.isEmpty()) {

                // 用于链式地址计算的临时变量
                long prevCustomCorePoolPhysAddr = 0L;
                long prevCustomCorePoolLogicalAddr = 0L;
                long prevCustomCorePoolSpaceSize = 0L;

                // 获取健康监控日志池的地址和大小作为第一个自定义核心池的计算基础
                // 依赖于 generateHmLogMemoryBlockSpaceConfigStruct (表36) 已经执行并更新了
                // globalCalculatedData
                if (globalCalculatedData.containsKey("LAST_HMLOG_SPACE_PHYS_ADDR") &&
                        globalCalculatedData.containsKey("LAST_HMLOG_SPACE_LOG_ADDR") &&
                        globalCalculatedData.containsKey("LAST_HMLOG_SPACE_SIZE")) {
                    prevCustomCorePoolPhysAddr = globalCalculatedData.get("LAST_HMLOG_SPACE_PHYS_ADDR");
                    prevCustomCorePoolLogicalAddr = globalCalculatedData.get("LAST_HMLOG_SPACE_LOG_ADDR");
                    prevCustomCorePoolSpaceSize = globalCalculatedData.get("LAST_HMLOG_SPACE_SIZE");
                }

                for (CoreOS.HardwareConfiguration.KernelRegion kernelRegion : kernelRegions) {
                    String poolName = kernelRegion.getfPoolName();
                    if (poolName == null || poolName.trim().isEmpty()) {
                        continue; // 跳过没有PoolName的kernelRegion
                    }

                    String baseFieldName = "CustomCorePoolMemBlockSpaceConfig_" + poolName + "_";

                    // 1. 空间名 (36字节) - PoolName属性值和"Pool"拼接而成
                    String spaceName = poolName + "Pool";
                    byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                    baos.write(spaceNameBytes);
                    fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                    // 2. 占位符 (4字节) - 固定值0
                    byte[] placeholderBytes = intToBytesLittleEndian(0);
                    baos.write(placeholderBytes);
                    fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                    // 3. 物理地址 (8字节)
                    // 当kernelRegion为第一个时，取值为健康监控日志池物理地址+健康监控日志池空间大小
                    // 当kernelRegion为第2至N个时，取值为前一个自定义核心池物理地址+前一个自定义核心池空间大小
                    long physicalAddress = prevCustomCorePoolPhysAddr + prevCustomCorePoolSpaceSize;
                    byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                    baos.write(physicalAddressBytes);
                    fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                    // 4. 逻辑地址 (8字节) - 计算方式与物理地址相同 (64位)
                    long logicalAddress = prevCustomCorePoolLogicalAddr + prevCustomCorePoolSpaceSize;
                    byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                    baos.write(logicalAddressBytes);
                    fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                    // 5. 空间大小 (8字节) - kernelRegion元素的Size属性值 (64位)
                    long spaceSize = kernelRegion.getSizeAsLong();
                    byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                    baos.write(spaceSizeBytes);
                    fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                    // 6. 页大小 (4字节) - 固定值0
                    byte[] pageSizeBytes = intToBytesLittleEndian(0);
                    baos.write(pageSizeBytes);
                    fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                    // 7. MMU属性 (4字节) - 根据WriteProtected属性值计算
                    // 当属性WriteProtected的属性值为0或者false时，取值为0x103
                    // 当属性WriteProtected的属性值为1或者true时，取值为0x101
                    int mmuAttribute = kernelRegion.isfWriteProtected() ? 0x101 : 0x103;
                    byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                    baos.write(mmuAttributeBytes);
                    fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                    // 8. 标记 (4字节) - 固定值0x8000002
                    byte[] flagBytes = intToBytesLittleEndian(0x8000002);
                    baos.write(flagBytes);
                    fieldMap.put(baseFieldName + "Flag", flagBytes);

                    // 8字节对齐
                    addAlignmentPadding(baos, fieldMap, baseFieldName);

                    // 更新用于下一个自定义核心池的物理地址、逻辑地址和空间大小
                    prevCustomCorePoolPhysAddr = physicalAddress;
                    prevCustomCorePoolLogicalAddr = logicalAddress;
                    prevCustomCorePoolSpaceSize = spaceSize;
                }

                // 更新 globalCalculatedData，存储最后一个自定义核心池的信息，以便后续可能的引用
                globalCalculatedData.put("LAST_CUSTOM_CORE_POOL_SPACE_PHYS_ADDR", prevCustomCorePoolPhysAddr);
                globalCalculatedData.put("LAST_CUSTOM_CORE_POOL_SPACE_LOG_ADDR", prevCustomCorePoolLogicalAddr);
                globalCalculatedData.put("LAST_CUSTOM_CORE_POOL_SPACE_SIZE", prevCustomCorePoolSpaceSize);
            }
        }

        return baos.toByteArray();
    }

    // 4.1.7.17 RAM 部署空间内存块空间配置存储结构 (表38)
    /**
     * generateRamPayloadMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateRamPayloadMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateRamPayloadMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "RamPayloadMemBlockSpaceConfig_";
        int structSize = calculateRamPayloadMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "ramPayloadPgPool”，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("ramPayloadPgPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，"ramPayloadRegion 元素的 Base_Address 属性值"
        long physicalAddress = 0L;
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            physicalAddress = parseHexOrDecLong(module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfRamPayloadRegionBaseAddress());
        }
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，取前序内存池结束地址 (64位)
        long logicalAddress = calculateRamPayloadBaseAddress(module, globalCalculatedData);
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，"ramPayloadRegion 元素的Size 属性值" (64位)
        long spaceSize = 0L;
        if (module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            // 从PhysicalMemory获取ramPayloadRegionSize，它是String类型，需要解析
            spaceSize = parseHexOrDecLong(
                    module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfRamPayloadRegionSize());
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x0"
        int mmuAttribute = 0x0;
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x8000002"
        int flag = 0x8000002;
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_RAM_PAYLOAD_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_RAM_PAYLOAD_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_RAM_PAYLOAD_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    /**
     * 计算 RAM 部署空间起始地址。
     * RAM 部署空间的逻辑地址紧跟最后一个自定义核心池；没有自定义核心池时，紧跟健康监控日志池。
     * 物理地址仍按表38直接取 XML 的 ramPayloadRegion/@Base_Address。
     */
    private long calculateRamPayloadBaseAddress(Module module, Map<String, Long> globalCalculatedData) {
        if (hasCustomCorePools(module)
                && globalCalculatedData.containsKey("LAST_CUSTOM_CORE_POOL_SPACE_LOG_ADDR")
                && globalCalculatedData.containsKey("LAST_CUSTOM_CORE_POOL_SPACE_SIZE")) {
            return globalCalculatedData.get("LAST_CUSTOM_CORE_POOL_SPACE_LOG_ADDR")
                    + globalCalculatedData.get("LAST_CUSTOM_CORE_POOL_SPACE_SIZE");
        }

        if (globalCalculatedData.containsKey("LAST_HMLOG_SPACE_LOG_ADDR")
                && globalCalculatedData.containsKey("LAST_HMLOG_SPACE_SIZE")) {
            return globalCalculatedData.get("LAST_HMLOG_SPACE_LOG_ADDR")
                    + globalCalculatedData.get("LAST_HMLOG_SPACE_SIZE");
        }

        // 防御性回退：正常流程会在此方法前生成健康监控日志池。
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            return parseHexOrDecLong(module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfRamPayloadRegionBaseAddress());
        }
        return 0L;
    }

    // 4.1.7.18 内核IO空间内存块空间配置存储结构 (表39)
    /**
     * generateKernelIOMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateKernelIOMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateKernelIOMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 由于KernelIO可能存在多个，这里应该循环生成每个KernelIO的内存块配置
        if (module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfKernelIOList() != null) {

            int kernelIOCount = 0;
            for (CoreOS.HardwareConfiguration.KernelIO kernelIO : module.getfCoreOS().getfHardwareConfiguration()
                    .getfKernelIOList()) {
                kernelIOCount++;
                String poolName = kernelIO.getKernelNameAsString();

                String instanceFieldName = "KernelIOMemBlockSpaceConfig_" + poolName + "_"; // 为每个KernelIO实例创建唯一前缀
                int structSize = calculateKernelIOMemoryBlockSpaceConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，固定值 "kernelIoPool"，不足用'\0'填充
                // 注意：PDF中此处是固定字符串，但XML映射处指向PoolName，存在歧义。按固定字符串处理。
                byte[] spaceNameBytes = fixedLengthAscii("kernelIoPool", 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(instanceFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(instanceFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节，"kernelI0 元素的 Base_Address 属性值"
                long physicalAddress = kernelIO.getfBaseAddress();
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(instanceFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，"kernelI0元素的 VirtualAddress 属性值" (64位)
                long logicalAddress = kernelIO.getfVirtualAddress();
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(instanceFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，"kernelI0 元素的 Size 属性值" (64位)
                long spaceSize = kernelIO.getfSize();
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(instanceFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值"0"
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(instanceFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节
                // 计算方式：0x3与CachePolicy对应值位或
                // 特殊情况：DEFAULT和未配置时直接使用0x100
                int mmuAttribute = 0x3; // 基础值
                String cachePolicy = kernelIO.getfCachePolicy();
                if (cachePolicy != null && !cachePolicy.trim().isEmpty()) {
                    switch (cachePolicy.toUpperCase()) {
                        case "OFF":
                            mmuAttribute |= 0x10;
                            break; // 0x3 | 0x10 = 0x13
                        case "COPY_BACK":
                            mmuAttribute |= 0x20;
                            break; // 0x3 | 0x20 = 0x23
                        case "WRITE_THRU":
                            mmuAttribute |= 0x30;
                            break; // 0x3 | 0x30 = 0x33
                        case "OFF+COHERENCY":
                            mmuAttribute |= 0x50;
                            break; // 0x3 | 0x50 = 0x53
                        case "COPY_BACK+COHERENCY":
                            mmuAttribute |= 0x60;
                            break; // 0x3 | 0x60 = 0x63
                        case "WRITE_THRU+COHERENCY":
                            mmuAttribute |= 0x70;
                            break; // 0x3 | 0x70 = 0x73
                        case "OFF+GUARDED":
                            mmuAttribute |= 0x90;
                            break; // 0x3 | 0x90 = 0x93
                        case "COPY_BACK+GUARDED":
                            mmuAttribute |= 0xA0;
                            break; // 0x3 | 0xA0 = 0xA3
                        case "WRITE_THRU+GUARDED":
                            mmuAttribute |= 0xB0;
                            break; // 0x3 | 0xB0 = 0xB3
                        case "OFF+GUARDED+COHERENCY":
                            mmuAttribute |= 0xD0;
                            break; // 0x3 | 0xD0 = 0xD3
                        case "COPY_BACK+GUARDED+COHERENCY":
                            mmuAttribute |= 0xE0;
                            break; // 0x3 | 0xE0 = 0xE3
                        case "WRITE_THRU+GUARDED+COHERENCY":
                            mmuAttribute |= 0xF0;
                            break; // 0x3 | 0xF0 = 0xF3
                        case "IO":
                            mmuAttribute |= 0x90;
                            break; // 0x3 | 0x90 = 0x93
                        case "DEFAULT":
                            mmuAttribute |= 0x100;
                            break;
                        default:
                            mmuAttribute |= 0x100;
                            break;
                    }
                } else {
                    mmuAttribute |= 0x90; // 未配置时，默认按IO处理：0x3 | 0x90 = 0x93
                }

                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(instanceFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) - 4字节，固定值"0x8000002"
                int flag = 0x8000002;
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(instanceFieldName + "Flag", flagBytes);

                // 每个KernelIO都进行8字节对齐
                addAlignmentPadding(baos, fieldMap, instanceFieldName);

                // Update global calculated data for potential chaining (per kernelIO)
                // This block's specific calculated data might be needed for subsequent kernelIO
                // if they are chained in a different way.
                // For general chaining, the master generator will pick up the last generated
                // block.
            }
        }
        // No specific LAST_KERNEL_IO_SPACE_PHYS_ADDR needed for the next block based on
        // the PDF chain

        return baos.toByteArray();
    }

    // 4.1.7.18.5 sharedIO内存块空间配置存储结构 (介于表39和表40之间)
    /**
     * generateSharedIOMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateSharedIOMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateSharedIOMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 获取CoreOS中的SharedIO配置
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfSharedIO() != null) {

            CoreOS.HardwareConfiguration.SharedIO sharedIO = module.getfCoreOS().getfHardwareConfiguration()
                    .getfSharedIO();
            String baseFieldName = "SharedIOMemBlockSpaceConfig_";

            // 1. 空间名 (36字节) - sharedIO元素的PoolName属性值 + "Pool"
            String poolName = sharedIO.getfPoolName();
            if (poolName == null || poolName.trim().isEmpty()) {
                poolName = "sharedIO"; // 默认值
            }
            String spaceName = poolName + "Pool";
            byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
            baos.write(spaceNameBytes);
            fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

            // 2. 占位符 (4字节) - 固定值0
            byte[] placeholderBytes = intToBytesLittleEndian(0);
            baos.write(placeholderBytes);
            fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

            // 3. 物理地址 (8字节) - sharedIO元素的BaseAddress属性值
            long physicalAddress = sharedIO.getfBaseAddress();
            byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
            baos.write(physicalAddressBytes);
            fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

            // 4. 逻辑地址 (8字节) - sharedIO元素的VirtualAddress属性值 (64位)
            long logicalAddress = sharedIO.getfVirtualAddress();
            byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
            baos.write(logicalAddressBytes);
            fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

            // 5. 空间大小 (8字节) - sharedIO元素的Size属性值 (64位)
            long spaceSize = sharedIO.getfSize();
            byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
            baos.write(spaceSizeBytes);
            fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

            // 6. 页大小 (4字节) - 固定值0
            byte[] pageSizeBytes = intToBytesLittleEndian(0);
            baos.write(pageSizeBytes);
            fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

            // 7. MMU属性 (4字节) - SharedIODescription时为0x93
            int mmuAttribute = 0x93;
            byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
            baos.write(mmuAttributeBytes);
            fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

            // 8. 标记 (4字节) - SharedIODescription时为0x100000
            int flag = 0x100000;
            byte[] flagBytes = intToBytesLittleEndian(flag);
            baos.write(flagBytes);
            fieldMap.put(baseFieldName + "Flag", flagBytes);

            // 更新 globalCalculatedData，存储sharedIO信息
            globalCalculatedData.put("SHARED_IO_SPACE_PHYS_ADDR", physicalAddress);
            globalCalculatedData.put("SHARED_IO_SPACE_LOG_ADDR", logicalAddress);
            globalCalculatedData.put("SHARED_IO_SPACE_SIZE", spaceSize);

            // 8字节对齐
            addAlignmentPadding(baos, fieldMap, baseFieldName);
        }

        return baos.toByteArray();
    }

    // 4.1.7.19 配置记录内存块空间配置存储结构 (表40)
    /**
     * generateConfigRecordMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateConfigRecordMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateConfigRecordMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "ConfigRecordMemBlockSpaceConfig_";
        int structSize = calculateConfigRecordMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值 "configRecordPool”，不足用'\0'填充
        byte[] spaceNameBytes = fixedLengthAscii("configRecordPool", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，"内核内存块物理地址+内核内存块空间大小"
        // 假设这里指的是CoreOS.HardwareConfiguration.PhysicalMemory的BaseAddress和kernelMemoryRegionSize
        long physicalAddress = 0L;
        long kernelMemBlockBase = 0L;
        long kernelMemBlockSize = 0L;

        if (module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            kernelMemBlockBase = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfBaseAddress();
            kernelMemBlockSize = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfKernelMemoryRegionSize();
        }
        physicalAddress = kernelMemBlockBase + kernelMemBlockSize; // 假设此为“内核内存块物理地址”和“内核内存块空间大小”

        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，"内核内存池物理地址+内核内存池空间大小" (64位)
        // 这里假设与物理地址计算相同
        long logicalAddress = physicalAddress; // 假设逻辑地址计算方式与物理地址相同
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，"kernelConfigRecordRegion 元素的Size 属性值" (64位)
        long spaceSize = 0L;
        if (module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            String kernelConfigRecordRegionSizeStr = module.getfCoreOS().getfHardwareConfiguration()
                    .getfPhysicalMemory().getfKernelConfigRecordRegionSize();
            spaceSize = parseHexOrDecLong(kernelConfigRecordRegionSizeStr);
        }
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值"0"
        byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x101"
        int mmuAttribute = 0x101;
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值"0x28000002"
        int flag = 0x28000002;
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_CONFIG_REC_SPACE_PHYS_ADDR", physicalAddress);
        globalCalculatedData.put("LAST_CONFIG_REC_SPACE_LOG_ADDR", logicalAddress);
        globalCalculatedData.put("LAST_CONFIG_REC_SPACE_SIZE", spaceSize);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        byte[] result = baos.toByteArray();
        return result;
    }

    // 4.1.7.20 分区操作系统共享库内存块空间配置存储结构 (表41)
    /**
     * generatePartitionOSSharedLibraryMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generatePartitionOSSharedLibraryMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePartitionOSSharedLibraryMemoryBlockSpaceConfigStruct(Module module,
            Map<String, byte[]> fieldMap, Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 用于跟踪最后一个共享库的物理地址和空间大小
        long lastSharedLibPhysAddr = 0L;
        long lastSharedLibSpaceSize = 0L;

        // 用于跟踪当前物理地址（跨共享库累加）
        long currentPhysicalAddress = 0L;

        // 初始化 currentPhysicalAddress 为 libRegionBaseAddress
        if (module.getfCoreOS() != null && module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {
            currentPhysicalAddress = parseHexOrDecLong(
                    module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory().getfLibRegionBaseAddress());
        }

        // 多个共享库，需要遍历
        if (module.getfSharedLibraries() != null) {
            for (Map.Entry<String, SharedLibrary> entry : module.getfSharedLibraries().entrySet()) {
                SharedLibrary sharedLibrary = entry.getValue();
                String baseFieldName = "PartitionOSSharedLibMemBlockSpaceConfig_" + sharedLibrary.getfNameAsString()
                        + "_";
                int structSize = calculatePartitionOSSharedLibraryMemoryBlockSpaceConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，"Name 属性值+"Pool""
                String spaceName = sharedLibrary.getfNameAsString() + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值"0"
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节
                // 第一个共享库使用 libRegionBaseAddress，后续共享库使用累加的物理地址
                long physicalAddress = currentPhysicalAddress;
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
                // PDF中此处是固定值0
                long logicalAddress = 0L;
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，"共享库各个段大小之和" (64位)
                // This requires iterating through the SharedLibrary's MemorySize fields (Text,
                // RoData, Data, Bss, PersistentData, PersistentBss)
                long totalSharedLibMemorySize = 0L;
                if (sharedLibrary.getfMemorySize() != null) {
                    totalSharedLibMemorySize += parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeText());
                    totalSharedLibMemorySize += parseHexOrDecLong(
                            sharedLibrary.getfMemorySize().getfMemorySizeRoData());
                    totalSharedLibMemorySize += parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeData());
                    totalSharedLibMemorySize += parseHexOrDecLong(sharedLibrary.getfMemorySize().getfMemorySizeBss());
                    totalSharedLibMemorySize += parseHexOrDecLong(
                            sharedLibrary.getfMemorySize().getfMemorySizePersistentData());
                    totalSharedLibMemorySize += parseHexOrDecLong(
                            sharedLibrary.getfMemorySize().getfMemorySizePersistentBss());
                }
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(totalSharedLibMemorySize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值"0"
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值"0x23"
                int mmuAttribute = 0x23;
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) - 4字节，固定值"0x8000002"
                int flag = 0x8000002;
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // 8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);

                // Update global calculated data for potential chaining (per shared library)
                globalCalculatedData.put(
                        "LAST_PARTITION_OS_SHARED_LIB_SPACE_PHYS_ADDR_" + sharedLibrary.getfNameAsString(),
                        physicalAddress);
                globalCalculatedData.put(
                        "LAST_PARTITION_OS_SHARED_LIB_SPACE_LOG_ADDR_" + sharedLibrary.getfNameAsString(),
                        logicalAddress);
                globalCalculatedData.put("LAST_PARTITION_OS_SHARED_LIB_SPACE_SIZE_" + sharedLibrary.getfNameAsString(),
                        totalSharedLibMemorySize);

                // 更新最后一个共享库的地址和大小（用于分区内存块的计算）
                lastSharedLibPhysAddr = physicalAddress;
                lastSharedLibSpaceSize = totalSharedLibMemorySize;

                // 更新 currentPhysicalAddress 为下一个共享库的起始地址
                currentPhysicalAddress = physicalAddress + totalSharedLibMemorySize;
            }
        }
        // 保存最后一个共享数据的物理地址和空间大小，供分区内存块使用
        // 遍历所有SharedData，累加它们的Size之和
        long totalSharedDataSize = 0L;
        if (module != null && module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty()) {
            for (Map.Entry<String, SharedData> entry : module.getfSharedDatas().entrySet()) {
                SharedData sharedData = entry.getValue();
                if (sharedData != null && sharedData.getfSharedDataDescription() != null) {
                    long size = sharedData.getfSharedDataDescription().getSizeAsLong();
                    totalSharedDataSize += size;
                }
            }
        }
        lastSharedLibSpaceSize = totalSharedDataSize;
        globalCalculatedData.put("LAST_PARTITION_OS_SHARED_LIB_SPACE_SIZE", lastSharedLibSpaceSize);

        // LAST_PARTITION_OS_SHARED_LIB_SPACE_PHYS_ADDR 只保存 userMemoryBaseAddress
        if (module != null && module.getfCoreOS() != null
                && module.getfCoreOS().getfHardwareConfiguration() != null
                && module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion() != null) {
            long userMemoryBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion()
                    .getfBaseAddress();
            globalCalculatedData.put("LAST_PARTITION_OS_SHARED_LIB_SPACE_PHYS_ADDR", userMemoryBaseAddress);
        } else {
            globalCalculatedData.put("LAST_PARTITION_OS_SHARED_LIB_SPACE_PHYS_ADDR", parseHexOrDecLong("0L"));
        }

        return baos.toByteArray();
    }

    /**
     * 4.1.7.21 共享数据内存块空间配置存储结构 (表42)
     * 
     * 根据XML路径：/Module/SharedDataRegions/SharedData
     * 生成共享数据内存块空间配置。此结构为可选项，只有当XML中配置了SharedData元素时才会生成。
     * 
     * 结构内容（按照表42规范）：
     * - 空间名：SharedData元素的Name属性值和"Pool"拼接而成，36字节，不足以"\0"填充
     * - 占位符：4字节，固定值0
     * - 物理地址：8字节，第一个为userMemoryRegion的Base_Address，后续为前一个共享数据物理地址+大小
     * - 逻辑地址：8字节，SharedDataDescription元素的VirtualAddress属性值 (64位)
     * - 空间大小：8字节，SharedDataDescription元素的Size属性值 (64位)
     * - 页大小：4字节，固定值0
     * - MMU属性：4字节，SharedDataDescription时为0x23，SharedIODescription时为0x93
     * - 标记：4字节，SharedDataDescription时为0x8000002，SharedIODescription时为0x100000
     */
    private byte[] generateSharedDataMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 检查是否存在SharedDataRegions配置
        if (module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty()) {

            // 用于链式地址计算的临时变量
            long prevSharedDataPhysAddr = 0L;
            long prevSharedDataSpaceSize = 0L;
            boolean isFirstSharedData = true;

            // 获取userMemoryRegion的Base_Address作为第一个SharedData的物理地址基础
            long userMemoryBaseAddress = 0L;
            if (module.getfCoreOS() != null &&
                    module.getfCoreOS().getfHardwareConfiguration() != null &&
                    module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion() != null) {
                userMemoryBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion()
                        .getfBaseAddress();
            }

            // 获取CoreOS中的SharedIO配置（用于SharedIODescription类型的SharedData）
            CoreOS.HardwareConfiguration.SharedIO coreOSSharedIO = null;
            if (module.getfCoreOS() != null &&
                    module.getfCoreOS().getfHardwareConfiguration() != null &&
                    module.getfCoreOS().getfHardwareConfiguration().getfSharedIO() != null) {
                coreOSSharedIO = module.getfCoreOS().getfHardwareConfiguration().getfSharedIO();
            }

            int sharedDataCount = 0;
            for (Map.Entry<String, com.nwpu.verify.entity.SharedData> sharedDataEntry : module.getfSharedDatas()
                    .entrySet()) {
                com.nwpu.verify.entity.SharedData sharedData = sharedDataEntry.getValue();
                String sharedDataName = sharedData.getfNameAsString();

                if (sharedDataName == null || sharedDataName.trim().isEmpty()) {
                    continue; // 跳过没有Name的SharedData
                }

                // 跳过sharedIO类型 -
                // sharedIO已在步骤18.5中单独处理（generateSharedIOMemoryBlockSpaceConfigStruct）
                if (sharedData.getfSharedIODescription() != null) {
                    continue;
                }

                sharedDataCount++;
                String baseFieldName = "SharedDataMemBlockSpaceConfig_" + sharedDataName + "_";

                // 1. 空间名 (36字节) - SharedData元素的Name属性值和"Pool"拼接而成
                String spaceName = sharedDataName + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (4字节) - 固定值0
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (8字节)
                // 当SharedData为第一个时，值为userMemoryRegion元素的Base_Address属性值
                // 当SharedData为第2个至N个时，值为前一个共享数据物理地址+前一个共享数据大小
                // 对于SharedIODescription，从CoreOS的SharedIO中获取Base_Address
                long physicalAddress;
                boolean isSharedIO = sharedData.getfSharedIODescription() != null;
                if (isSharedIO && coreOSSharedIO != null) {
                    // SharedIODescription类型，使用CoreOS中SharedIO的BaseAddress
                    physicalAddress = coreOSSharedIO.getfBaseAddress();
                } else if (isSharedIO && coreOSSharedIO == null) {
                    System.out.println(
                            "WARNING [MemBlockConfig]: SharedIO '" + sharedDataName + "' 但CoreOS中没有SharedIO配置!");
                    physicalAddress = isFirstSharedData ? userMemoryBaseAddress
                            : (prevSharedDataPhysAddr + prevSharedDataSpaceSize);
                } else if (isFirstSharedData) {
                    physicalAddress = userMemoryBaseAddress;
                } else {
                    physicalAddress = prevSharedDataPhysAddr + prevSharedDataSpaceSize;
                }
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (8字节) - SharedDataDescription或SharedIODescription元素的VirtualAddress属性值
                // (64位)
                long logicalAddress = 0L;
                if (sharedData.getfSharedIODescription() != null) {
                    // SharedIODescription优先使用应用层定义的VirtualAddress
                    logicalAddress = sharedData.getfSharedIODescription().getVirtualAddressAsLong();
                    // 如果应用层VirtualAddress为0，尝试使用CoreOS的SharedIO VirtualAddress
                    if (logicalAddress == 0L && coreOSSharedIO != null) {
                        logicalAddress = coreOSSharedIO.getfVirtualAddress();
                    }
                } else if (sharedData.getfSharedDataDescription() != null) {
                    logicalAddress = sharedData.getfSharedDataDescription().getVirtualAddressAsLong();
                }
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (8字节) - SharedDataDescription或SharedIODescription元素的Size属性值 (64位)
                long spaceSize = 0L;
                if (isSharedIO && coreOSSharedIO != null) {
                    // SharedIODescription类型，使用CoreOS中SharedIO的Size
                    spaceSize = coreOSSharedIO.getfSize();
                } else if (sharedData.getfSharedDataDescription() != null) {
                    spaceSize = sharedData.getfSharedDataDescription().getSizeAsLong();
                }
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (4字节) - 固定值0
                byte[] pageSizeBytes = intToBytesLittleEndian(0);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU属性 (4字节) - SharedDataDescription时为0x23，SharedIODescription时为0x93
                int mmuAttribute = 0x23; // 默认为SharedDataDescription
                if (sharedData.getfSharedIODescription() != null) {
                    mmuAttribute = 0x93; // SharedIODescription
                }
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (4字节) - SharedDataDescription时为0x8000002，SharedIODescription时为0x100000
                int flag = 0x8000002; // 默认为SharedDataDescription
                if (sharedData.getfSharedIODescription() != null) {
                    flag = 0x100000; // SharedIODescription
                }
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // 更新用于下一个共享数据的物理地址和空间大小（仅对非SharedIO类型）
                if (!isSharedIO) {
                    prevSharedDataPhysAddr = physicalAddress;
                    prevSharedDataSpaceSize = spaceSize;
                }
                isFirstSharedData = false;

                // 每个SharedData都进行8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);
            }

            // 更新 globalCalculatedData，存储最后一个共享数据的信息，以便后续可能的引用
            globalCalculatedData.put("LAST_SHARED_DATA_SPACE_PHYS_ADDR", prevSharedDataPhysAddr);
            globalCalculatedData.put("LAST_SHARED_DATA_SPACE_LOG_ADDR", 0L); // 逻辑地址不用于链式计算
            globalCalculatedData.put("LAST_SHARED_DATA_SPACE_SIZE", prevSharedDataSpaceSize);

        }

        return baos.toByteArray();
    }

    // 4.1.7.22 分区内存块空间配置存储结构 (表43)
    /**
     * generatePartitionMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generatePartitionMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generatePartitionMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 多个Partition，需要遍历
        long prevPartitionPhysAddr = 0;
        long prevPartitionSpaceSize = 0;
        if (module.getfPartitions() != null) {
            // Keep track of the previous partition memory block's physical address and size
            // for chaining
            prevPartitionPhysAddr = 0L;
            prevPartitionSpaceSize = 0L;

            boolean isFirstPartition = true;

            // Get last shared library physical address and size for the first partition's
            // calculation
            long lastSharedLibPhysAddr = globalCalculatedData
                    .getOrDefault("LAST_PARTITION_OS_SHARED_LIB_SPACE_PHYS_ADDR", 0L);
            long lastSharedLibSpaceSize = globalCalculatedData.getOrDefault("LAST_PARTITION_OS_SHARED_LIB_SPACE_SIZE",
                    0L);

            for (Map.Entry<String, Partition> entry : module.getfPartitions().entrySet()) {
                Partition partition = entry.getValue();
                // 跳过PseudoPartition，这是不需要的
                if (partition.isFIsPseudoPartition()) {
                    continue;
                }

                String baseFieldName = "PartitionMemBlockSpaceConfig_" + partition.getfNameAsString() + "_";
                int structSize = calculatePartitionMemoryBlockSpaceConfigSize();

                // 1. 空间名 (fSpaceName) - 36字节，"Name 属性值和”Pool”拼接而成"
                String spaceName = partition.getfNameAsString() + "Pool";
                byte[] spaceNameBytes = fixedLengthAscii(spaceName, 36, (char) 0);
                baos.write(spaceNameBytes);
                fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

                // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
                byte[] placeholderBytes = intToBytesLittleEndian(0);
                baos.write(placeholderBytes);
                fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

                // 3. 物理地址 (fPhysicalAddress) - 8字节
                long physicalAddress = 0L;
                if (isFirstPartition) {
                    // 当Partition 为第一个时,值为最后一个共享库物理地址+最后一个共享库空间大小
                    physicalAddress = lastSharedLibPhysAddr + lastSharedLibSpaceSize;
                } else {
                    // 当Partition 为第2个至N个时,值为前一个分区池物理地址+前一个分区的最大占用空间
                    physicalAddress = prevPartitionPhysAddr + prevPartitionSpaceSize;

                }
                byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
                baos.write(physicalAddressBytes);
                fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

                // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值"0" (64位)
                long logicalAddress = 0L; // Fixed value from PDF
                byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
                baos.write(logicalAddressBytes);
                fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

                // 5. 空间大小 (fSpaceSize) - 8字节，"Settings 元素的RequiredMemorySize 属性" (64位)
                long spaceSize = 0L;
                if (partition.getfSettings() != null) {
                    spaceSize = partition.getfSettings().getfRequiredMemorySize();
                }
                byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
                baos.write(spaceSizeBytes);
                fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

                // 6. 页大小 (fPageSize) - 4字节，固定值“0”
                byte[] pageSizeBytes = longTo4BytesLittleEndian(0L);
                baos.write(pageSizeBytes);
                fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

                // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值“0x23”
                int mmuAttribute = 0x23; // Fixed value from PDF
                byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
                baos.write(mmuAttributeBytes);
                fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

                // 8. 标记 (fFlag) - 4字节，固定值“0x8000002”
                int flag = 0x8000002; // Fixed value from PDF
                byte[] flagBytes = intToBytesLittleEndian(flag);
                baos.write(flagBytes);
                fieldMap.put(baseFieldName + "Flag", flagBytes);

                // Update for next iteration
                prevPartitionPhysAddr = physicalAddress;
                prevPartitionSpaceSize = spaceSize;
                isFirstPartition = false;

                // 每个Partition都进行8字节对齐
                addAlignmentPadding(baos, fieldMap, baseFieldName);
            }
        }
        // Update global calculated data for potential chaining
        globalCalculatedData.put("LAST_PARTITION_SPACE_PHYS_ADDR", prevPartitionPhysAddr);
        globalCalculatedData.put("LAST_PARTITION_SPACE_SIZE", prevPartitionSpaceSize);

        return baos.toByteArray();
    }

    // 4.1.7.23 空内存块空间配置存储结构 (表44)
    /**
     * generateEmptyMemoryBlockSpaceConfigStruct
     *
     * @brief
     * 功能: 执行 generateEmptyMemoryBlockSpaceConfigStruct 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] generateEmptyMemoryBlockSpaceConfigStruct(Module module, Map<String, byte[]> fieldMap,
            Map<String, Long> globalCalculatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String baseFieldName = "EmptyMemBlockSpaceConfig_";
        int structSize = calculateEmptyMemoryBlockSpaceConfigSize();

        // 1. 空间名 (fSpaceName) - 36字节，固定值空字符串
        byte[] spaceNameBytes = fixedLengthAscii("", 36, (char) 0);
        baos.write(spaceNameBytes);
        fieldMap.put(baseFieldName + "SpaceName", spaceNameBytes);

        // 2. 占位符 (fPlaceholder) - 4字节，固定值“0”
        byte[] placeholderBytes = intToBytesLittleEndian(0);
        baos.write(placeholderBytes);
        fieldMap.put(baseFieldName + "Placeholder", placeholderBytes);

        // 3. 物理地址 (fPhysicalAddress) - 8字节，固定值整数“-1”
        long physicalAddress = -1L;
        byte[] physicalAddressBytes = longTo8BytesLittleEndian(physicalAddress);
        baos.write(physicalAddressBytes);
        fieldMap.put(baseFieldName + "PhysicalAddress", physicalAddressBytes);

        // 4. 逻辑地址 (fLogicalAddress) - 8字节，固定值整数"-1" (64位)
        long logicalAddress = -1L;
        byte[] logicalAddressBytes = longTo8BytesLittleEndian(logicalAddress);
        baos.write(logicalAddressBytes);
        fieldMap.put(baseFieldName + "LogicalAddress", logicalAddressBytes);

        // 5. 空间大小 (fSpaceSize) - 8字节，固定值整数"-1" (64位)
        long spaceSize = -1L;
        byte[] spaceSizeBytes = longTo8BytesLittleEndian(spaceSize);
        baos.write(spaceSizeBytes);
        fieldMap.put(baseFieldName + "SpaceSize", spaceSizeBytes);

        // 6. 页大小 (fPageSize) - 4字节，固定值整数"-1"
        long pageSize = -1L;
        byte[] pageSizeBytes = longTo4BytesLittleEndian(pageSize);
        baos.write(pageSizeBytes);
        fieldMap.put(baseFieldName + "PageSize", pageSizeBytes);

        // 7. MMU 属性 (fMmuAttribute) - 4字节，固定值整数"-1"
        int mmuAttribute = -1;
        byte[] mmuAttributeBytes = intToBytesLittleEndian(mmuAttribute);
        baos.write(mmuAttributeBytes);
        fieldMap.put(baseFieldName + "MmuAttribute", mmuAttributeBytes);

        // 8. 标记 (fFlag) - 4字节，固定值整数"-1"
        int flag = -1;
        byte[] flagBytes = intToBytesLittleEndian(flag);
        baos.write(flagBytes);
        fieldMap.put(baseFieldName + "Flag", flagBytes);

        // 8字节对齐
        addAlignmentPadding(baos, fieldMap, baseFieldName);

        return baos.toByteArray();
    }

    // =======================================================================
    // 主控生成方法：按顺序生成所有内存块空间配置数据
    // =======================================================================
    /**
     * generateAllMemoryBlockConfigs
     *
     * @brief
     * 功能: 执行generateAllMemoryBlockConfigs处理逻辑。
     *
     * @param[in]  module: 参数
     * @param[in]  fieldMap: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public byte[] generateAllMemoryBlockConfigs(Module module, Map<String, byte[]> fieldMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 用于在不同内存块生成方法之间传递计算结果的全局状态Map
        // 存储键名示例: "LAST_HMLOG_SPACE_PHYS_ADDR", "LAST_HMLOG_SPACE_SIZE" 等
        Map<String, Long> globalCalculatedData = new HashMap<>();

        // 按照PDF中4.1.7节指定的顺序依次生成
        // 顺序：核心内存块权限、配置记录、核心页、端口缓冲区、健康监控日志记录、RAM部署空间、内核IO空间、
        // 分区内存块、分区操作系统共享库、共享数据、自定义核心池、
        // 核心内存块空间、核心页内存块空间、端口缓冲区、健康监控日志记录、自定义核心池、RAM部署空间、内核IO空间、
        // 配置记录、分区操作系统共享库、共享数据、分区内存块、空内存块。

        // 1. 核心内存块权限配置存储结构 (表22)
        byte[] coreMemPermBytes = generateCoreMemoryBlockPermissionConfigStruct(module, fieldMap, globalCalculatedData);
        baos.write(coreMemPermBytes);

        // 2. 配置记录内存块权限配置存储结构 (表23)
        byte[] configRecPermBytes = generateConfigRecordMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(configRecPermBytes);

        // 3. 核心页内存块权限配置存储结构 (表24)
        byte[] corePagePermBytes = generateCorePageMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(corePagePermBytes);

        // 4. 端口缓冲区内存块权限配置存储结构 (表25)
        byte[] portBufferPermBytes = generatePortBufferMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(portBufferPermBytes);

        // 5. 健康监控日志记录内存块权限配置存储结构 (表26)
        byte[] hmLogPermBytes = generateHmLogMemoryBlockPermissionConfigStruct(module, fieldMap, globalCalculatedData);
        baos.write(hmLogPermBytes);

        // 6. RAM 部署空间内存块权限配置存储结构 (表27)
        byte[] ramPayloadPermBytes = generateRamPayloadMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(ramPayloadPermBytes);

        // 7. 内核IO空间内存块权限配置存储结构 (表28)
        byte[] kernelIOPermBytes = generateKernelIOMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(kernelIOPermBytes);

        // 8. 分区内存块权限配置存储结构 (表29)
        byte[] partitionMemPermBytes = generatePartitionMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(partitionMemPermBytes);

        // 9. 分区操作系统共享库内存块权限配置存储结构 (表30)
        byte[] partitionOSLibPermBytes = generatePartitionOSSharedLibraryMemoryBlockPermissionConfigStruct(module,
                fieldMap, globalCalculatedData);
        baos.write(partitionOSLibPermBytes);

        // 10. 共享数据内存块权限配置存储结构 (表31)
        int beforeSharedDataPerm = baos.size();
        byte[] sharedDataPermBytes = generateSharedDataMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(sharedDataPermBytes);

        // 11. 自定义核心池内存块权限配置存储结构 (表32)
        byte[] customCorePoolPermBytes = generateCustomCorePoolMemoryBlockPermissionConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(customCorePoolPermBytes);

        // 12. 核心内存块空间配置存储结构 (表33)
        byte[] coreMemSpaceBytes = generateCoreMemoryBlockSpaceConfigStruct(module, fieldMap, globalCalculatedData);
        baos.write(coreMemSpaceBytes);

        // 13. 核心页内存块空间配置存储结构 (表34)
        byte[] corePageSpaceBytes = generateCorePageMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(corePageSpaceBytes);

        // 14. 端口缓冲区内存块空间配置存储结构 (表35)
        byte[] portBufferSpaceBytes = generatePortBufferMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(portBufferSpaceBytes);

        // 15. 健康监控日志记录内存块空间配置存储结构 (表36)
        byte[] hmLogSpaceBytes = generateHmLogMemoryBlockSpaceConfigStruct(module, fieldMap, globalCalculatedData);
        baos.write(hmLogSpaceBytes);

        // 16. 自定义核心池内存块空间配置存储结构 (表37) - 可选，只有配置了kernelRegion才生成
        if (hasCustomCorePools(module)) {
            byte[] customCorePoolSpaceBytes = generateCustomCorePoolMemoryBlockSpaceConfigStruct(module, fieldMap,
                    globalCalculatedData);
            baos.write(customCorePoolSpaceBytes);
        }

        // 17. RAM 部署空间内存块空间配置存储结构 (表38) - 可选，只有配置了ramPayloadRegion才生成
        if (hasRamPayloadRegion(module)) {
            byte[] ramPayloadSpaceBytes = generateRamPayloadMemoryBlockSpaceConfigStruct(module, fieldMap,
                    globalCalculatedData);
            baos.write(ramPayloadSpaceBytes);
        }

        // 18. 内核IO空间内存块空间配置存储结构 (表39)
        // 实际应根据 module.getfCoreOS().getfHardwareConfiguration().getfKernelIOList()
        // 是否为空来判断
        byte[] kernelIOSpaceBytes = generateKernelIOMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(kernelIOSpaceBytes);

        // 18.5 sharedIO内存块空间配置存储结构 (表39之后, 表40之前)
        // 根据问题报告单PR_02，sharedIO应该出现在内核IO和配置记录之间
        if (hasSharedIO(module)) {
            byte[] sharedIOSpaceBytes = generateSharedIOMemoryBlockSpaceConfigStruct(module, fieldMap,
                    globalCalculatedData);
            baos.write(sharedIOSpaceBytes);
        }

        // 19. 配置记录内存块空间配置存储结构 (表40)
        byte[] configRecSpaceBytes = generateConfigRecordMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(configRecSpaceBytes);

        // 20. 分区操作系统共享库内存块空间配置存储结构 (表41)
        byte[] partitionOSLibSpaceBytes = generatePartitionOSSharedLibraryMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(partitionOSLibSpaceBytes);

        // 21. 共享数据内存块空间配置存储结构 (表42) - 可选，只有配置了SharedData才生成
        if (hasSharedData(module)) {
            byte[] sharedDataSpaceBytes = generateSharedDataMemoryBlockSpaceConfigStruct(module, fieldMap,
                    globalCalculatedData);
            baos.write(sharedDataSpaceBytes);
        }

        // 22. 分区内存块空间配置存储结构 (表43)
        // 实际应根据 module.getfPartitions() 是否为空来判断
        byte[] partitionMemSpaceBytes = generatePartitionMemoryBlockSpaceConfigStruct(module, fieldMap,
                globalCalculatedData);
        baos.write(partitionMemSpaceBytes);

        // 23. 空内存块空间配置存储结构 (表44)
        byte[] emptyMemSpaceBytes = generateEmptyMemoryBlockSpaceConfigStruct(module, fieldMap, globalCalculatedData);
        baos.write(emptyMemSpaceBytes);

        return baos.toByteArray();
    }

    // =======================================================================
    // 辅助判断方法：判断可选配置是否存在
    // =======================================================================
    /**
     * hasCustomCorePools
     *
     * @brief
     * 功能: 执行hasCustomCorePools处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private boolean hasCustomCorePools(Module module) {
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            List<CoreOS.HardwareConfiguration.KernelRegion> kernelRegions = module.getfCoreOS()
                    .getfHardwareConfiguration().getfPhysicalMemory().getfKernelRegions();

            return kernelRegions != null && !kernelRegions.isEmpty();
        }
        return false;
    }
    /**
     * hasRamPayloadRegion
     *
     * @brief
     * 功能: 执行hasRamPayloadRegion处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private boolean hasRamPayloadRegion(Module module) {
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            String ramPayloadRegionBaseAddress = module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory()
                    .getfRamPayloadRegionBaseAddress();

            // 检查是否配置了有效的基地址
            return ramPayloadRegionBaseAddress != null &&
                    !ramPayloadRegionBaseAddress.trim().isEmpty() &&
                    !ramPayloadRegionBaseAddress.equals("0") &&
                    !ramPayloadRegionBaseAddress.equals("0x0");
        }
        return false;
    }
    /**
     * hasSharedData
     *
     * @brief
     * 功能: 执行hasSharedData处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private boolean hasSharedData(Module module) {
        return module.getfSharedDatas() != null && !module.getfSharedDatas().isEmpty();
    }
    /**
     * hasSharedIO
     *
     * @brief
     * 功能: 执行hasSharedIO处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private boolean hasSharedIO(Module module) {
        return module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfSharedIO() != null;
    }

    // =======================================================================
    // 辅助计数方法：计算各种实体的数量
    // =======================================================================
    /**
     * getRealPartitionCount
     *
     * @brief
     * 功能: 执行getRealPartitionCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private int getRealPartitionCount(Module module) {
        if (module.getfPartitions() == null) {
            return 0;
        }

        int count = 0;
        for (Partition partition : module.getfPartitions().values()) {
            if (!partition.isFIsPseudoPartition()) {
                count++;
            }
        }
        return count;
    }
    /**
     * getSharedLibraryCount
     *
     * @brief
     * 功能: 执行getSharedLibraryCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private int getSharedLibraryCount(Module module) {
        if (module.getfSharedLibraries() == null) {
            return 0;
        }
        return module.getfSharedLibraries().size();
    }
    /**
     * getSharedDataCount
     *
     * @brief
     * 功能: 执行getSharedDataCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private int getSharedDataCount(Module module) {
        if (module.getfSharedDatas() == null) {
            return 0;
        }
        return module.getfSharedDatas().size();
    }
    /**
     * getCustomCorePoolCount
     *
     * @brief
     * 功能: 执行getCustomCorePoolCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private int getCustomCorePoolCount(Module module) {
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfPhysicalMemory() != null) {

            List<CoreOS.HardwareConfiguration.KernelRegion> kernelRegions = module.getfCoreOS()
                    .getfHardwareConfiguration().getfPhysicalMemory().getfKernelRegions();

            if (kernelRegions != null) {
                return kernelRegions.size();
            }
        }
        return 0;
    }
    /**
     * getKernelIOCount
     *
     * @brief
     * 功能: 执行getKernelIOCount处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private int getKernelIOCount(Module module) {
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfKernelIOList() != null) {

            List<CoreOS.HardwareConfiguration.KernelIO> kernelIOList = module.getfCoreOS().getfHardwareConfiguration()
                    .getfKernelIOList();

            return kernelIOList.size();
        }
        return 0;
    }
}
